package com.bi.propakSansSeparator.impl;

import java.util.Locale;

import com.ur.urcap.api.contribution.ViewAPIProvider;
import com.ur.urcap.api.contribution.program.ContributionConfiguration;
import com.ur.urcap.api.contribution.program.CreationContext;
import com.ur.urcap.api.contribution.program.ProgramAPIProvider;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;
import com.ur.urcap.api.domain.data.DataModel;


public class ProPakProgramNodeService implements SwingProgramNodeService<ProPakProgramNodeContribution, ProPakProgramNodeView> {
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return "mainProPakNode";
	}

	@Override
	public void configureContribution(ContributionConfiguration configuration) {
		// TODO Auto-generated method stub
		configuration.setChildrenAllowed(true);
	}

	@Override
	public String getTitle(Locale locale) {
		return "Program";
	}

	@Override
	public ProPakProgramNodeView createView(ViewAPIProvider apiProvider) {
		// TODO Auto-generated method stub
		return new ProPakProgramNodeView(apiProvider);
	}

	@Override
	public ProPakProgramNodeContribution createNode(ProgramAPIProvider apiProvider, ProPakProgramNodeView view,
			DataModel model, CreationContext context) {
		// TODO Auto-generated method stub
		return new ProPakProgramNodeContribution(apiProvider, view, model, context);
	}
}
