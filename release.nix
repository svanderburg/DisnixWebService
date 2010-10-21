{ nixpkgs ? /etc/nixos/nixpkgs }:

let
  jobs = rec {
    tarball =
      { DisnixWebService ? {outPath = ./.; rev = 1234;}
      , officialRelease ? false
      }:

      with import nixpkgs {};

      releaseTools.sourceTarball {
        name = "DisnixWebService-tarball";
        version = builtins.readFile ./version;
        src = DisnixWebService;
        inherit officialRelease;
	distPhase =
	''
	  mkdir -p ../bin/DisnixWebService-$version
	  cp -av * ../bin/DisnixWebService-$version
	  cd ../bin
	  ensureDir $out/tarballs
	  tar cfvj $out/tarballs/DisnixWebService-$version.tar.bz2 DisnixWebService-$version
	'';
      };

    build =
      { tarball ? jobs.tarball {}
      , system ? "x86_64-linux"
      }:

      with import nixpkgs { inherit system; };

      releaseTools.nixBuild {
        name = "DisnixWebService";
	version = builtins.readFile ./version;
        src = tarball;
	PREFIX = ''''${env.out}'';
        AXIS2_LIB = "${axis2}/lib";
        AXIS2_WEBAPP = "${axis2}/webapps/axis2";
        DBUS_JAVA_LIB = "${dbus_java}/share/java";
        patchPhase =
	''
          sed -i -e "s|#JAVA_HOME=|JAVA_HOME=${jdk}|" \
                 -e "s|#AXIS2_LIB=|AXIS2_LIB=${axis2}/lib|" \
	      scripts/disnix-soap-client
        '';
        buildPhase = "ant generate.war";
        installPhase = "ant install";
	checkPhase = "echo hello";
        buildInputs = [ apacheAnt ];
      };
      
    tests = 
      { nixos ? /etc/nixos/nixos
      , disnix ? (import ../../disnix/trunk/release.nix {}).build {}
      , disnix_activation_scripts ? (import ../../disnix-activation-scripts/trunk/release.nix {}).build {}
      }:
      
      let
        DisnixWebService = build { system = "x86_64-linux"; };
	tests = ./tests;
      in
      with import "${nixos}/lib/testing.nix" { inherit nixpkgs; system = "x86_64-linux"; services = null; };
      
      {
        install = simpleTest {
	  nodes = {
	    server =
	      {pkgs, config, ...}:
	      
	      {
	        virtualisation.writableStore = true;
		
                services.dbus.enable = true;
                services.dbus.packages = [ disnix ];
	    
	        jobs.disnix =
                  { description = "Disnix server";

                    startOn = "started dbus";

                    script =
                      ''
                        export PATH=/var/run/current-system/sw/bin:/var/run/current-system/sw/sbin
                        export HOME=/root
			
                        ${disnix}/bin/disnix-service --activation-modules-dir=${disnix_activation_scripts}/libexec/disnix/activation-scripts
                      '';
	           };

		ids.gids = { disnix = 200; };
		users.extraGroups = [ { gid = 200; name = "disnix"; } ];
                users.extraUsers = [ { name = "tomcat"; group = "tomcat"; description = "Tomcat user"; extraGroups = [ "disnix" ]; } ];

	        services.tomcat.enable = true;
		services.tomcat.javaOpts = "-Djava.library.path=${pkgs.libmatthew_java}/lib/jni";
                services.tomcat.catalinaOpts = "-Xms64m -Xmx256m";
                services.tomcat.sharedLibs = [ "${DisnixWebService}/share/java/DisnixConnection.jar"
                                               "${pkgs.dbus_java}/share/java/dbus.jar" ];
                services.tomcat.webapps = [ DisnixWebService ];
	      };
	      
	    client =
	      {pkgs, config, ...}:
	      
	      {
	        virtualisation.writableStore = true;	  
	        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv ];
	      };
	  };	    
	  testScript = 
	    ''
	      startAll;
	      
	      # Wait until tomcat is started and the DisnixWebService is activated
              $server->waitForJob("tomcat");
	      $server->waitForFile("/var/tomcat/webapps/DisnixWebService");
	      	      
	      # Run test-cases
	      
	      my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid /nix/store/invalid");
	      
	      if($result =~ /\/nix\/store\/invalid/) {
	          print "/nix/store/invalid is invalid\n";
	      } else {
	          die "/nix/store/invalid should be invalid\n";
	      }
	      
	      #$client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock");
	      #$client->mustFail("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock");
	      #$client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock");
	      #$client->mustFail("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock");
	      	      
	      #### Test copy closure
	      
	      my $testService = $client->mustSucceed("NIXPKGS_ALL=${nixpkgs}/pkgs/top-level/all-packages.nix nix-build ${tests}/testservice.nix");
	      $server->mustFail("nix-store --check-validity $testService");
	      $client->mustSucceed("disnix-copy-closure --interface disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --to $testService");
	      $server->mustSucceed("nix-store --check-validity $testService");
	      
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --activate --arguments foo=foo --arguments bar=bar --type echo $testService");
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --deactivate --arguments foo=foo --arguments bar=bar --type echo $testService");
	    '';
	};
      };
  };
in jobs
