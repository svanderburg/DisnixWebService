#let pkgs = import /etc/nixos/nixpkgs/pkgs/top-level/all-packages.nix { };
#let pkgs = import /home/sander/nixsc/nix/nixpkgs/trunk/pkgs/top-level/all-packages.nix { };
let pkgs = import /home/sander/nix/nixpkgs/trunk/pkgs/top-level/all-packages.nix { };
in
with pkgs;

rec
{
    helloworld = 
	stdenv.mkDerivation {
	    name = "hello-2.1.1";
	    src = fetchurl {
		url = mirror://gnu/hello/hello-2.1.1.tar.gz;
		md5 = "70c9ccf9fac07f762c24f2df2290784d";
	    };
	    buildInputs = [perl];    
	};
}
