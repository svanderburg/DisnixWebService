{
  testTarget1 = {
    properties = {
      hostname = "testTarget1";
      targetEPR = http://testTarget1:8080/DisnixWebService/services/DisnixWebService;
    };
    targetProperty = "targetEPR";
    clientInterface = "disnix-soap-client"; # This machine requires the disnix-soap-client to connect to the remote web service
  };
  
  testTarget2 = {
    properties = {
      hostname = "testTarget2";
    };
    targetProperty = "hostname";
    clientInterface = "disnix-ssh-client"; # This machine requires the disnix-ssh-client to connect to the remote machine through SSH
  };
}
