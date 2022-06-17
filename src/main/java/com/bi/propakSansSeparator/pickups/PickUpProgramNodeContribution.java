package com.bi.propakSansSeparator.pickups;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.ur.urcap.api.contribution.ProgramNodeContribution;
import com.ur.urcap.api.contribution.installation.CreationContext.NodeCreationType;
import com.ur.urcap.api.contribution.program.CreationContext;
import com.ur.urcap.api.contribution.program.ProgramAPIProvider;
import com.ur.urcap.api.domain.UserInterfaceAPI;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.io.DigitalIO;
import com.ur.urcap.api.domain.io.IOModel;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.undoredo.UndoRedoManager;
import com.ur.urcap.api.domain.undoredo.UndoableChanges;
import com.ur.urcap.api.domain.userinteraction.RobotPositionCallback;
import com.ur.urcap.api.domain.userinteraction.robot.movement.MovementCompleteEvent;
import com.ur.urcap.api.domain.userinteraction.robot.movement.RobotMovement;
import com.ur.urcap.api.domain.userinteraction.robot.movement.RobotMovementCallback;
import com.ur.urcap.api.domain.value.Pose;
import com.ur.urcap.api.domain.value.PoseFactory;
import com.ur.urcap.api.domain.value.jointposition.JointPositionFactory;
import com.ur.urcap.api.domain.value.jointposition.JointPositions;
import com.ur.urcap.api.domain.value.jointposition.JointPosition;
import com.ur.urcap.api.domain.value.simple.Angle;
import com.ur.urcap.api.domain.value.simple.Length;

public class PickUpProgramNodeContribution implements ProgramNodeContribution{
	private final ProgramAPIProvider apiProvider;
	private DataModel dataModel;
	private PickUpProgramNodeView view;
	private final UndoRedoManager undoRedoManager;
	private final RobotMovement robotMovement;
	private final IOModel ioModel;
	private DigitalIO valve_out;
	private DigitalIO blowOff_out;
	private DigitalIO suction_in;
	
	private static boolean isOn;
	
	private static long DELAY_BLOWOFF = 1000L;			// in milliseconds
	private static long DELAY_READ_INPUT = 1000L;

	private final static int NUMBER_OF_POSITIONS = 8;
	
	private boolean[] definedPickUps = new boolean[NUMBER_OF_POSITIONS];

	private final int number;
	private final int pallet;

	private final PoseFactory poseFactory;
	private final JointPositionFactory jointPositionFactory;
	private final Pose emptyPose;
	private final JointPositions emptyJoints;

	private final static String fileName = "pickup_clipboard.txt";
	
	public PickUpProgramNodeContribution(ProgramAPIProvider provider, DataModel model, PickUpProgramNodeView view, CreationContext context) {
		this.apiProvider = provider;
		this.dataModel = model;
		this.view = view;

		if (view.CT_FACH) {
			this.pallet = 2;
			view.CT_FACH = false;
		} else {
			view.NODE_COUNT++;
			this.pallet = 1;
			view.CT_FACH = true;
		}

		this.number = view.NODE_COUNT;
		if (view.NODE_COUNT > 24 && !view.CT_FACH) {
			view.resetNodeCounter();
		}
		this.undoRedoManager = apiProvider.getProgramAPI().getUndoRedoManager();
		this.ioModel = apiProvider.getProgramAPI().getIOModel();
		
		this.robotMovement = apiProvider.getUserInterfaceAPI().getUserInteraction().getRobotMovement();

		this.poseFactory = apiProvider.getProgramAPI().getValueFactoryProvider().getPoseFactory();
		this.emptyPose = poseFactory.createPose(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Length.Unit.M, Angle.Unit.RAD);

		this.jointPositionFactory = apiProvider.getProgramAPI().getValueFactoryProvider().getJointPositionFactory();
		this.emptyJoints = jointPositionFactory.createJointPositions(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Angle.Unit.RAD);
		
		if (context.getNodeCreationType().equals(NodeCreationType.NEW)) {
			for (int i = 0; i < NUMBER_OF_POSITIONS; i++) {
				dataModel.set("PICKUP_" + String.valueOf(i), this.emptyPose);
				dataModel.set("PICKUP_" + String.valueOf(i) + "_J", this.emptyJoints);
				definedPickUps[i] = false;
			}
		}
	}
	
	@Override
	public void openView() {
		for (int i = 0; i < NUMBER_OF_POSITIONS; ++i) {
			Pose p = dataModel.get("PICKUP_" + String.valueOf(i), emptyPose);
			setPoseDefined("PICKUP_" + String.valueOf(i));
		}
		valve_out = getDigitalIO("digital_out[0]");
		suction_in = getDigitalIO("digital_in[0]");
		view.valveButtonColor(valve_out.getValue());

		if (existsClipboard()) {
			view.clipboardSetText(refNumberFromClipboard(), palletFromClipboard());
		}
	}

	@Override
	public void closeView() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getTitle() {
		String pallet = (this.pallet == 1) ? "left" : "right";
		String title = "";
		if("pl".equals(Locale.getDefault().getLanguage())) {
			if (pallet.equals("left")) {
				title = "Pobrania Paleta Lewa";
			} else {
				title = "Pobrania Paleta Prawa";
			}
		} else {
			if (pallet.equals("left")) {
				title = "Pickups Left Pallet";
			} else {
				title = "Pickups Right Pallet";
			}
		}
		return title;
	}

	@Override
	public boolean isDefined() {
		return (this.number != 0) ? true : false;
	}

	@Override
	public void generateScript(ScriptWriter writer) {
		String rNum = Integer.toString(this.number);
		String pallet = (this.pallet == 1) ? "left" : "right";
		for (int i = 0; i < NUMBER_OF_POSITIONS; ++i) {
			int n = i + 1;
			String key = "PICKUP_" + String.valueOf(i);
			Pose p = dataModel.get(key, this.emptyPose);
			if (p != this.emptyPose) {
				writer.appendLine("global boxPose_" + n + "_" + pallet + "_" + rNum + " = " + p.toString());
				//System.out.println("global boxPose_" + n + "_" + pallet + "_" + rNum + " = " + p.toString());
			} else {
				writer.appendLine("global boxPose_" + n + "_" + pallet + "_" + rNum + " = p[ 0.4, -0.58, 0.59, 0.12, 3.14, 0.0 ]");
			}
		}

		view.clipboardResetText();
		view.enablePasteButton(false);
		deleteClipboard();
	}
	
	public void setPositionAction(final String posKey) {
		this.undoRedoManager.recordChanges(new UndoableChanges() {
			@Override
			public void executeChanges() {
				UserInterfaceAPI uiapi = apiProvider.getUserInterfaceAPI();
				uiapi.getUserInteraction().getUserDefinedRobotPosition(new RobotPositionCallback() {
					@Override
					public void onOk(Pose pose, JointPositions q) {
						dataModel.set(posKey, pose);
						dataModel.set(posKey + "_J", q);
						dataModel.set(posKey + "_DEF", true);

						setPoseDefined(posKey);
					}
				});
			}
			
		});
	}
	
	public void moveToPositionAction(final String key) {
		JointPositions q = dataModel.get(key + "_J", this.emptyJoints);

		robotMovement.requestUserToMoveRobot(q, new RobotMovementCallback() {
			@Override
			public void onComplete(MovementCompleteEvent event) {
				
			}
		});
	}

	public void deletePickUp(final String key) {
		int i = Integer.parseInt(key.substring(key.length() - 1));
		view.setDefinedIcon(i, false);
		
		this.undoRedoManager.recordChanges(new UndoableChanges() {

			@Override
			public void executeChanges() {
				dataModel.set(key + "_DEF", false);
			}
			
		});
	}
	
	public void activateValveAction() {
		valve_out = getDigitalIO("digital_out[0]");
		blowOff_out = getDigitalIO("digital_out[1]");
		suction_in = getDigitalIO("digital_in[0]");
		
		final Timer timerOnDelay = new Timer();
		
		if (valve_out.getValue()) {
			valve_out.setValue(false);
			blowOff_out.setValue(true);
			this.view.valveButtonColor(false);
			//this.view.suctionButtonIndicatorColor(suction_in.getValue());
			
			// turn off the blow-off after a delay
			timerOnDelay.schedule(new TimerTask() {
				public void run() {
					blowOff_out.setValue(false);
				}
			}, DELAY_BLOWOFF);
		} else {
			valve_out.setValue(true);
			this.view.valveButtonColor(true);
			//this.view.suctionButtonIndicatorColor(suction_in.getValue());
		}
		
		// after a specified delay, read the suction input and update the indicator
		timerOnDelay.schedule(new TimerTask() {
			public void run() {
				setSuctionIndicator(suction_in.getValue());
			}
		}, DELAY_READ_INPUT);
	}
	
	private void setSuctionIndicator(boolean isSet) {
		this.view.suctionButtonIndicatorColor(isSet);
	}

	private void setPoseDefined(final String key) {
		final int i = (key.contains("ABOVE")) ? NUMBER_OF_POSITIONS : Integer.parseInt(key.substring(key.length() - 1));
		
		boolean check = dataModel.get(key + "_DEF", false);

		if (check) {
			view.setDefinedIcon(i, true);
		} else if (!check) {
			view.setDefinedIcon(i, false);
		}
			
	}
	
	public DigitalIO getDigitalIO(String defaultName) {
		Collection<DigitalIO> IOCollection = ioModel.getIOs(DigitalIO.class);
		int IO_count = IOCollection.size();
		if (IO_count > 0) {
			Iterator<DigitalIO> itr = IOCollection.iterator();
			while(itr.hasNext()) {
				DigitalIO thisIO = itr.next();
				String thisDefaultName = thisIO.getDefaultName();
				if (thisDefaultName.contentEquals(defaultName)) {
					return thisIO;
				}
			}
		}
		return null;
	}

	public void copy(){
		//	Create the file
		try {
			File clipboard = new File(fileName);
			clipboard.createNewFile();
		} catch (IOException e) {
			System.out.println("IOExcpetion thrown.");
			e.printStackTrace();
		}

		//	Write the proper contents of the file
		try {
			FileWriter writer = new FileWriter(fileName);
			String palletStr = (this.pallet == 1) ? "Left" : "Right";
			String contents = this.number + "," + palletStr + "\n";
			for (int i = 0; i < NUMBER_OF_POSITIONS; ++i) {
				String key = "PICKUP_" + String.valueOf(i);
				Pose p = dataModel.get(key, this.emptyPose);
				JointPositions q = dataModel.get(key + "_J", this.emptyJoints);
				JointPosition qp[] = q.getAllJointPositions();

				String tempQ = "";
				for (JointPosition j : qp) {
					tempQ = tempQ + j.getPosition(Angle.Unit.RAD) + ",";
				}
				tempQ = tempQ.substring(0, tempQ.length() - 1);

				contents += p.toString() + "\n" + tempQ + "\n";

				boolean isDef = dataModel.get(key + "_DEF", false);
				
				contents += Boolean.toString(isDef) + "\n";
			}

			writer.write(contents);
			writer.close();
			// System.out.print(contents);
		} catch (IOException e) {
			System.out.println("IOException on writing to the file.");
			e.printStackTrace();
		}

		String palletString = "";
		if ("pl".equals(Locale.getDefault().getLanguage())) {
			palletString = (this.pallet == 1) ? "Lewa" : "Prawa";
		} else {
			palletString = (this.pallet == 1) ? "Left" : "Right";
		}
		view.clipboardSetText(this.number, palletString);
		view.enablePasteButton(true);
	}

	public void paste(){
		undoRedoManager.recordChanges(new UndoableChanges() {
			@Override
			public void executeChanges() {
				try {
					File clipboard = new File(fileName);
					Scanner readHead = new Scanner(clipboard);
					// skip the first line, cause there, the reference number and pallet side are held.
					readHead.nextLine();
					int i = 0;
					while (readHead.hasNextLine()) {
						String data = readHead.nextLine();
						Pose p = poseFromString(data);
						String key = "PICKUP_" + String.valueOf(i);
						dataModel.set(key, p);

						data = readHead.nextLine();
						JointPositions q = jointPositionsFromString(data);
						dataModel.set(key + "_J", q);

						data = readHead.nextLine();
						boolean isDef = Boolean.parseBoolean(data);
						dataModel.set(key + "_DEF", isDef);
						setPoseDefined(key);

						++i;
					}
					readHead.close();
				} catch (FileNotFoundException e) {
					System.out.println("FileNotFoundException thrown.");
					e.printStackTrace();
				}
			}
		});
	}

	private void deleteClipboard() {
		File clipboard = new File(fileName);
		clipboard.delete();
	}

	private boolean existsClipboard() {
		File clipboard = new File(fileName);
		boolean ret = false;
		try {
			ret = clipboard.createNewFile();
		} catch (IOException e) {
			System.out.println("Error while deleting the file");
			e.printStackTrace();
		}
		
		if (ret) {
			deleteClipboard();
		}
		
		return !ret;
	}

	//	HEPLERS FOR COPYING AND PASTING
	//	Mostly stuff to work with strings, since copying is done via a text file
	private int refNumberFromClipboard() {
		File clipboard = new File(fileName);
		int ret = 0;
		
		try {
			Scanner readHead = new Scanner(clipboard);
			String data = readHead.nextLine();
			String splitData[] = data.split(",");
			ret = Integer.valueOf(splitData[0]);
			readHead.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return ret;
	}

	private String palletFromClipboard() {
		File clipboard = new File(fileName);
		String ret = "kappa";
		String temp = "";
		
		try {
			Scanner readHead = new Scanner(clipboard);
			String data = readHead.nextLine();
			String splitData[] = data.split(",");
			temp = splitData[1];
			readHead.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if ("pl".equals(Locale.getDefault().getLanguage())) {
			if (temp.contains("Left")) {
				ret = "Lewa";
			} else if (temp.contains("Right")) {
				ret = "Prawa";
			}
		} else {
			ret = temp;
		}
		
		return ret;
	}

	private Pose poseFromString(String input) {
		double dPose[] = new double[6];
		String subInput = input.substring(2, input.length() - 1);
		String splitInput[] = subInput.split(",");
		
		for (int i = 0; i < 6; ++i) {
			dPose[i] = Double.parseDouble(splitInput[i]);
		}
		
		Pose p = poseFactory.createPose(dPose[0], dPose[1], dPose[2], dPose[3], dPose[4], dPose[5], Length.Unit.M, Angle.Unit.RAD);
		return p;
	}

	private JointPositions jointPositionsFromString(String input) {
		double dJoints[] = new double[6];
		String splitInput[] = input.split(",");
		
		for (int i = 0; i < 6; ++i) {
			dJoints[i] = Double.parseDouble(splitInput[i]);
		}
		
		JointPositions q = jointPositionFactory.createJointPositions(dJoints[0], dJoints[1], dJoints[2], dJoints[3], dJoints[4], dJoints[5], Angle.Unit.RAD);
		return q;
	}
}
