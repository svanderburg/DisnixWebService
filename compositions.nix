let compositions = rec {
    pkgs = import /home/sander/nixsc/nix/nixpkgs/trunk/pkgs/top-level/all-packages.nix { };
    
    hello = {
	recurseForDerivations = true;
	pkg = pkgs.hello;
	dependsOn = [];
    };
}; in compositions
