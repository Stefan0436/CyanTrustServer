# ConnectiveHTTP ${pretty_name}
${description}

<br />

# Building the ${pretty_name}
This project is build using gradle, but it supplies the wrapper so you dont need to install it.<br />
What you do need is Java JDK, if you have it installed, proceed with the following commands depending
on your operating system.

## Building for linux
Run the following commands:

```bash
chmod +x gradlew createlocalserver.sh
./createlocalserver.sh
./gradlew createEclipseLaunches build
cd build/libs
```

## Building for windows
Run the following commands:

```batch
gradlew.bat createEclipseLaunches build
cd build\\libs
```

<br />

# Installing the ${pretty_name}
The installation depends on your server software.<br />
Though it is not too different for each server type.

## Installing for the standalone server
${install.standalone.desc}

## Installing for ASF RaTs! (Remote Advanced Testing Suite)
${install.rats.desc}<br />
After which, add the following line to the `${install.rats.config.key}` key of the `components.ccfg` file:

```
# File: components.ccfg
# ...
${install.rats.config.key}> {

    # ...

    ${install.rats.config.entry}> '${name}-${version}.jar'
${otherclasses.rats}
    # ...

}
# ...
```

$moduleConfigAdditionsStandalone

## Post-installation module dependencies
This project HEAVILY depends on the usermanager module in order to run, you can download the latest version directly from our maven repository: [Download UserManager Version 1.0.0.A7](https://aerialworks.ddns.net/maven/org/asf/connective/usermanager/UserManager/1.0.0.A7) (select the normal jarfile)

### Enabling the login service
This module uses the UserManager Authentication Aervice API to authenticate some requests. In order for this to fully function, our service needs to be registered. Due to a bug, registering the service at first boot is not possible, you will need to add it to the modules block after first boot:

```
# File: server/rats.ccfg
# ...

modules> {
    # ...
    UserManager-AuthServices> {
        # ...
        
        # Add the following:
        cyan.trust.service.login> 'cyan.trust.service.login'
        
        # ...
    }
    # ...
}

# ...
```

Reload the server and the service should be available.

<br />

# Version Notice:
This module was build targeting ASF Connective version ${connectiveversion},
it may not work on newer or older versions.

# Copyright Notice:
This project is licensed under the ${license} license.<br />
Copyright(c) ${buildyear} ${authorname}.<br />
${copyrightsuffix}.<br />
<br />
This project uses the ConnectiveHTTP libraries.<br />
Copyright(c) 2021 AerialWorks Software Foundation.
