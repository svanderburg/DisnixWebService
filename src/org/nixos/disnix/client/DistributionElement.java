package org.nixos.disnix.client;

public class DistributionElement
{
	public String component;
	
	public String targetEPR;

	public DistributionElement()
	{
	}
	
	public DistributionElement(String component, String targetEPR)
	{
		setComponent(component);
		setTargetEPR(targetEPR);
	}
	
	public String getComponent()
	{
		return component;
	}

	public void setComponent(String component)
	{
		this.component = component;
	}

	public String getTargetEPR()
	{
		return targetEPR;
	}

	public void setTargetEPR(String targetEPR)
	{
		this.targetEPR = targetEPR;
	}
}
