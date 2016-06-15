{ nixpkgs ? <nixpkgs>
, DisnixWebService ? {outPath = ./.; rev = 1234;}
, officialRelease ? false
, systems ? [ "i686-linux" "x86_64-linux" ]
, disnixJobset ? import ../disnix/release.nix { inherit nixpkgs systems officialRelease; }
, dysnomiaJobset ? import ../dysnomia/release.nix { inherit nixpkgs systems officialRelease; }
}:

let
  pkgs = import nixpkgs {};
  
  jobs = rec {
    tarball =
      pkgs.releaseTools.sourceTarball {
        name = "DisnixWebService-tarball";
        version = builtins.readFile ./version;
        src = DisnixWebService;
        inherit officialRelease;
        buildInputs = [ pkgs.libxml2 pkgs.libxslt pkgs.dblatex (pkgs.dblatex.tex or pkgs.tetex) pkgs.apacheAnt pkgs.jdk pkgs.help2man pkgs.doclifter ];
        PREFIX = ''''${env.out}'';
        AXIS2_LIB = "${pkgs.axis2}/lib";
        DBUS_JAVA_LIB = "${pkgs.dbus_java}/share/java";
        
        preConfigure = ''
          # TeX needs a writable font cache.
          export VARTEXFONTS=$TMPDIR/texfonts
        '';

        distPhase = ''
          cd doc
          make docbookrng=${pkgs.docbook5}/xml/rng/docbook docbookxsl=${pkgs.docbook5_xsl}/xml/xsl/docbook
          cp index.pdf $out
          cd ..
          ant install.doc
          ant install.javadoc
          
          echo "doc manual $out/share/doc/DisnixWebService" >> $out/nix-support/hydra-build-products
          echo "doc-pdf manual $out/index.pdf" >> $out/nix-support/hydra-build-products
          echo "doc api $out/share/doc/javadoc" >> $out/nix-support/hydra-build-products
          
          mkdir -p ../bin/DisnixWebService-$version
          rm -Rf `find . -name .git`
          mv * ../bin/DisnixWebService-$version
          cd ../bin
          ensureDir $out/tarballs
          tar cfvj $out/tarballs/DisnixWebService-$version.tar.bz2 DisnixWebService-$version
        '';
      };

    build =
      pkgs.lib.genAttrs systems (system:
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
          buildInputs = [ apacheAnt jdk ];
      });
      
    tests = 
      let
        dysnomia = builtins.getAttr (builtins.currentSystem) (dysnomiaJobset.build);
        disnix = builtins.getAttr (builtins.currentSystem) (disnixJobset.build);
        DisnixWebService = builtins.getAttr (builtins.currentSystem) build;
      in
      {
        install = import ./tests/install.nix {
          inherit nixpkgs dysnomia disnix DisnixWebService;
        };
        
        multiproto = import ./tests/multiproto.nix {
          inherit nixpkgs dysnomia disnix DisnixWebService;
        };
        
        auth = import ./tests/auth.nix {
          inherit nixpkgs dysnomia disnix DisnixWebService;
        };
        
        snapshots = import ./tests/snapshots.nix {
          inherit (pkgs) stdenv;
          inherit nixpkgs dysnomia disnix DisnixWebService;
        };
      };
    
    release = pkgs.releaseTools.aggregate {
      name = "DisnixWebService-${tarball.version}";
      constituents = [
        tarball
      ]
      ++ map (system: builtins.getAttr system build) systems
      ++ [
        tests.install
        tests.multiproto
        tests.auth
        tests.snapshots
      ];
      meta.description = "Release-critical builds";
    };
  };
in
jobs
