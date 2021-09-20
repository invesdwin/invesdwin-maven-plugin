# install eclipse
sudo rm -rf /opt/eclipse/
sudo tar xvzf eclipse-*.tar.gz -C /opt/
sudo cp /opt/eclipse/plugins/org.eclipse.platform_*/eclipse48.png /opt/eclipse
sudo chown $USER:$USER -R /opt/eclipse
# give it more ram
sudo sed -i s/-XX:MaxPermSize=[0-9]*m/-XX:MaxPermSize=256m/ /opt/eclipse/eclipse.ini
sudo sed -i s/-Xms[0-9]*m/-Xms2g/ /opt/eclipse/eclipse.ini
sudo sed -i s/-Xmx[0-9]*m/-Xmx5g/ /opt/eclipse/eclipse.ini
# create a launcher
echo "cd /opt/eclipse && ./eclipse \$*" | sudo tee /usr/local/bin/eclipse
sudo chmod +x /usr/local/bin/eclipse
# install javahl (optional)
sudo apt-get -y install libsvn-java
echo "-Djava.library.path=/usr/lib/jni:/usr/lib/x86_64-linux-gnu/jni/" | sudo tee -a /opt/eclipse/eclipse.ini
echo "-Dfile.encoding=UTF-8" | sudo tee -a /opt/eclipse/eclipse.ini
echo "--add-modules=ALL-SYSTEM" | sudo tee -a /opt/eclipse/eclipse.ini
