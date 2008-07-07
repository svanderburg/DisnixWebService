{ stdenv, lib, distribution }:

stdenv.mkDerivation rec {
    name = "distribution-export";
    
    generateDistributionElement = item: ''<dist composition="${item.composition.pkg.name}" machine="${item.machine.name}" />'';
    
    concatDistributionElements = elements: lib.concatStringsSep "\n" elements;
    
    distrElements = map generateDistributionElement distribution;
    
    outputXML = ''<?xml version="1.0" encoding="utf-8">
    <distribution>
    '' +
    concatDistributionElements distrElements +
    ''
    </distribution>
    '';
    
    builder = ./builder.sh;
}
