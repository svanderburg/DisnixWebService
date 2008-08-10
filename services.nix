{distribution}:

rec {
    pkgs = import (builtins.getEnv "NIXPKGS_ALL") { };
    
    hello = {
	recurseForDerivations = true;
	pkg = pkgs.hello;
	dependsOn = [];
    };
}
