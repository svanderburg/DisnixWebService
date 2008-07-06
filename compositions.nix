let compositions = rec {
    pkgs = import ./test-pkg.nix;
    
    helloworld = {
	recurseForDerivations = true;
	pkg = pkgs.helloworld;
	dependsOn = [];
    };
}; in compositions
