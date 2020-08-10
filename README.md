# Remote-run-server
System for allowing clients to run some code (currently Java and Python) on a possibly GPU accelerated server. The system is using docker with nvidia gpu pass-through. The server is can be run in two modes ether in master or in worker/slave mode. when running in master mode a worker is spawned as well so launching both is not necessary



## Requirements

For running in ether slave or master mode:

- [docker](https://docs.docker.com/engine/install/#server) and [docker-compose](https://docs.docker.com/compose/install/) has to be installed. 
- If the worker is going to be gpu accelerated, it is going to need the [nvidia doker toolkit](https://github.com/NVIDIA/nvidia-docker). 
- In the config folder rename the main.env.example -> main.env and the nginx_certbot.env.example -> nginx_certbot.env . The values in main.env needs to be filled in with whatever config you need. the  nginx_certbot.env  file is probably fine but you may want to uncomment the staging option while developing
- The system system_resources.yaml needs to be filled it tels the application how mutch resources you are ok with docker using.

If you want to run more than one worker you will need:
- some sort of network disk system is required to share the save data dir. chose whatever system you are comfortable with given that the application is kind of state based ish makes write conflicts practically not possible. I personally just use the nfs-kernel-server because it is easy and works. It is important that the volume is mounted to the Remote-run-server/save_data **Before** start_(master/worker) is run.
- A NIC with a local adress to allow other workers to connect to the host DB it is **strongly** recommended not to use the public interface if so it is very important to change the default password currently used


## Usage
Download and decompress the release and If all the requirements above are met,  just launch the ``` start_master.sh ``` to (big suprise) start the master and launch the ``` start_worker.sh ``` to start in worker mode. If you compile the project yourself you have to have the [Remote-run-client](https://github.com/Remote-run/Remote-run-client) installed in your local maven repo


### Notes


- The resource tags in ```ConfigFiles/resource_tags.yaml ``` can be changed while running **but** some editors like vim and gedit does replace the file when saving instead of changing is thereby braking the docker binding to the file. This does not brake anything but it will make the key unchangeable until next restart of the master. tldr; use nano to change the resource keys in runtime
