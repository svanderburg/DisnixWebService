let pkgs = import (builtins.getEnv "NIXPKGS_ALL") {};
in
with pkgs;

stdenv.mkDerivation {
  name = "testService";
  buildCommand =
  ''
    echo "testService" > $out
  '';
}
