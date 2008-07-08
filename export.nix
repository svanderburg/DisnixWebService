{ stdenv, distribution }:

stdenv.mkDerivation rec {
    name = "distribution-export";        
    outputXML = builtins.toXML distribution;
    builder = ./builder.sh;
}
