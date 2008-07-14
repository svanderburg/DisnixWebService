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
	
	public boolean equals(Object object)
	{
		if(object instanceof DistributionElement)
		{
			DistributionElement e = (DistributionElement)object;
			
			return this.getComponent().equals(e.getComponent()) &&
				   this.getTargetEPR().equals(e.getTargetEPR());
		}
		else
			return false;
	}
}
