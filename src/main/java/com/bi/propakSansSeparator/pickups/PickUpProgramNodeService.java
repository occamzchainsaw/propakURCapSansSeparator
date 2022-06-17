package com.bi.propakSansSeparator.pickups;

import java.util.Locale;

import com.ur.urcap.api.contribution.ViewAPIProvider;
import com.ur.urcap.api.contribution.program.ContributionConfiguration;
import com.ur.urcap.api.contribution.program.CreationContext;
import com.ur.urcap.api.contribution.program.ProgramAPIProvider;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;
import com.ur.urcap.api.domain.data.DataModel;

public class PickUpProgramNodeService implements SwingProgramNodeService<PickUpProgramNodeContribution, PickUpProgramNodeView>{
	@Override
	public String getId() {
		return "pickupNode";
	}

	@Override
	public void configureContribution(ContributionConfiguration configuration) {
		// TODO Auto-generated method stub
		configuration.setChildrenAllowed(false);
		configuration.setUserInsertable(false);
	}

	@Override
	public String getTitle(Locale locale) {
		if("pl".equals(Locale.getDefault().getLanguage())) {
			return "Pozycje Pobrań";
		} else {
			return "Pickup Positions";
		}
	}

	@Override
	public PickUpProgramNodeView createView(ViewAPIProvider apiProvider) {
		return new PickUpProgramNodeView(apiProvider);
	}

	@Override
	public PickUpProgramNodeContribution createNode(ProgramAPIProvider apiProvider, PickUpProgramNodeView view,
			DataModel model, CreationContext context) {
		return new PickUpProgramNodeContribution(apiProvider, model, view, context);
	}
}
