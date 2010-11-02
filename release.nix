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
	buildInputs = [ libxml2 libxslt dblatex tetex apacheAnt ];
	PREFIX = ''''${env.out}'';
	
	preConfigure = ''
	  # TeX needs a writable font cache.
          export VARTEXFONTS=$TMPDIR/texfonts
	'';

	distPhase = ''
	  cd doc
	  make docbookrng=${docbook5}/xml/rng/docbook docbookxsl=${docbook5_xsl}/xml/xsl/docbook
	  cp index.pdf $out
	  cd ..
	  ant install.doc
	  echo "doc manual $out/share/doc/DisnixWebService" >> $out/nix-support/hydra-build-products
	  echo "doc-pdf manual $out/index.pdf" >> $out/nix-support/hydra-build-products
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

	        services.tomcat.enable = true;
		services.tomcat.extraGroups = [ "disnix" ];
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
	      	      
	      # Check invalid path. We query an invalid path from the service
	      # which should return the path we have given.
	      # This test should succeed.
	      
	      my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid /nix/store/invalid");
	      
	      if($result =~ /\/nix\/store\/invalid/) {
	          print "/nix/store/invalid is invalid\n";
	      } else {
	          die "/nix/store/invalid should be invalid\n";
	      }
	      
	      # Check invalid path. We query a valid path from the service
	      # which should return nothing in this case.
	      # This test should succeed.
	      
	      my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}");
	      
	      # Query requisites test. Queries the requisites of the bash shell
	      # and checks whether it is part of the closure.
	      # This test should succeed.
	      
	      my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-requisites ${pkgs.bash}");
	      
	      if($result =~ /bash/) {
	          print "${pkgs.bash} is in the closure\n";
	      } else {
	          die "${pkgs.bash} should be in the closure!\n";
	      }
	      
	      # Realise test. First a bash derivation file is instantiated,
	      # then it is realised. This test should succeed.
	      
	      my $result = $server->mustSucceed("nix-instantiate ${nixpkgs} -A bash");
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --realise $result");
	      
	      # Export test. Exports the closure of the bash shell on the server
	      # and then imports it on the client. This test should succeed (BROKEN).
	      
	      #my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export --remotefile ${pkgs.bash}");
	      #$client->mustSucceed("nix-store --import < $result");
	      
	      # Import test. First we create a manifest, then we take the
	      # closure of the target2Profile on the client. Then it imports the
	      # closure into the Nix store of the server. This test should
	      # succeed.
	      
	      my $result = $client->mustSucceed("NIXPKGS_ALL=${nixpkgs} disnix-manifest --target-property targetEPR -s ${tests}/services.nix -i ${tests}/infrastructure.nix -d ${tests}/distribution.nix");
	      my @manifestClosure = split('\n', $client->mustSucceed("nix-store -qR $result"));
	      my @target2Profile = grep(/\-testTarget2/, @manifestClosure);
	      
	      $server->mustFail("nix-store --check-validity @target2Profile");
	      $client->mustSucceed("nix-store --export \$(nix-store -qR @target2Profile) > /root/target2Profile.closure");
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import --localfile /root/target2Profile.closure");
	      $server->mustSucceed("nix-store --check-validity @target2Profile");
	      
	      # Set test. Adds the testTarget2 profile as only derivation into 
	      # the Disnix profile. We first set the profile, then we check
	      # whether the profile is part of the closure.
	      # This test should succeed.
	      
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --set --profile default @target2Profile");
	      my @defaultProfileClosure = split('\n', $server->mustSucceed("nix-store -qR /nix/var/nix/profiles/disnix/default"));
	      my @closure = grep("@target2Profile", @defaultProfileClosure);
	      
	      if("@closure" == "") {
	          print "@target2Profile is part of the closure\n";
	      } else {
	          die "@target2Profile should be part of the closure\n";
	      }
	      
	      # Query installed test. Queries the installed services in the 
	      # profile, which has been set in the previous testcase.
	      # testService2 should be in there. This test should succeed.
	      
	      my @closure = split('\n', $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-installed --profile default"));
	      my @service = grep(/\-testService2/, @closure);
	      
	      if("@service" == "") {
	          print "@service is installed in the default profile\n";
	      } else {
	          die "@service should be installed in the default profile\n";
	      }
	      
	      # Collect garbage test. This test should succeed.
	      # Testcase disabled, as this is very expensive.
	      # $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --collect-garbage");

	      # Lock test. This test should succeed.
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock");
	      
	      # Lock test. This test should fail, since the service instance is already locked.
	      $client->mustFail("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock");
	      
	      # Unlock test. This test should succeed, so that we can release the lock.
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock");
	      
	      # Unlock test. This test should fail as the lock has already been released.
	      $client->mustFail("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock");
	      
	      # Use the echo type to activate a service.
	      # We use the testService1 service defined in the manifest.
	      # This test should succeed.
	      
	      my @testService1 = grep(/\-testService1/, @manifestClosure);
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --activate --arguments foo=foo --arguments bar=bar --type echo @testService1");
	      
	      # Deactivate the same service using the echo type. This test should succeed.
	      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --deactivate --arguments foo=foo --arguments bar=bar --type echo @testService1");
	    '';
	};
      };
  };
in jobs
