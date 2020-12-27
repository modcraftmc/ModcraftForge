echo "Installing clear server"

rm -r cleanserver
mkdir cleanserver

cp projects/forge/build/libs/forge-1.15.2-31.3.0-installer.jar cleanserver
java -jar cleanserver/forge-1.15.2-31.3.0-installer.jar --installServer cleanserver
java -jar -Xmx1G cleanserver/forge-1.15.2-31.3.0.jar nogui

rm forge-1.15.2-31.3.0-installer.jar.log