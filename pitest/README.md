# AdaMu-Extended Version
AdaMu is an implementation of adaptive mutation testing on top of PIT mutation testing tool.

## Setup

### Install the 3rd party library for AdaMu
```
mvn install:install-file \
    -Dfile=lib/javastat_beta1.4.jar \
    -DgroupId=javastat \
    -DartifactId=javastat \
    -Dversion=1.4.beta \
    -Dpackaging=jar \
    -DgeneratePom=true


mvn install:install-file \
    -Dfile=lib/beast.jar \
    -DgroupId=dr.app \
    -DartifactId=beast \
    -Dversion=1.0 \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn install:install-file \
    -Dfile=lib/BigDecimalMath.jar \
    -DgroupId=BigDecimalMath \
    -DartifactId=BigDecimalMath \
    -Dversion=1.0 \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn install:install-file \
    -Dfile=lib/flanagan.jar \
    -DgroupId=flanagan \
    -DartifactId=flanagan \
    -Dversion=1.0 \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn install:install-file \
    -Dfile=lib/guava-11.0.2.jar \
    -DgroupId=guava \
    -DartifactId=guava \
    -Dversion=11.0.2 \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn install:install-file \
    -Dfile=lib/jtransforms-2.4.jar \
    -DgroupId=jtransforms \
    -DartifactId=jtransforms \
    -Dversion=2.4 \
    -Dpackaging=jar \
    -DgeneratePom=true

mvn install:install-file \
    -Dfile=lib/ssj.jar \
    -DgroupId=ssj \
    -DartifactId=ssj \
    -Dversion=1.0 \
    -Dpackaging=jar \
    -DgeneratePom=true
```
