package org.nixos.disnix.client;

public class DistributionElement
{
	public String service;
	
	public String targetEPR;

	public DistributionElement()
	{
	}
	
	public DistributionElement(String service, String targetEPR)
	{
		setService(service);
		setTargetEPR(targetEPR);
	}
	
	public String getService()
	{
		return service;
	}

	public void setService(String service)
	{
		this.service = service;
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
			
			return this.getService().equals(e.getService()) &&
				   this.getTargetEPR().equals(e.getTargetEPR());
		}
		else
			return false;
	}
}
