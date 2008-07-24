{ stdenv, distribution }:

stdenv.mkDerivation rec {
    name = "distribution-export";
    distributionExport = map (entry: { component = entry.component.pkg.outPath; target = entry.target.targetEPR; } ) distribution;
    outputXML = builtins.toXML distributionExport;
    builder = ./builder.sh;
}
