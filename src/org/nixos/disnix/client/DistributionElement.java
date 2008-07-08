package org.nixos.disnix.client;

public class DistributionElement
{
	public String composition;
	
	public String targetEPR;

	public DistributionElement()
	{
	}
	
	public DistributionElement(String composition, String targetEPR)
	{
		setComposition(composition);
		setTargetEPR(targetEPR);
	}
	
	public String getComposition()
	{
		return composition;
	}

	public void setComposition(String composition)
	{
		this.composition = composition;
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
