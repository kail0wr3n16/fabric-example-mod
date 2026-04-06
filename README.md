# Client Loaded Mod (Fabric)

Minimal Fabric client mod for the latest stable Minecraft target in this template.

## What it does

When your player joins a world, the mod sends this chat message:

`Client loaded successfully`

## Requirements

- JDK 25 (because this template targets Minecraft `26.1`)
- Internet connection for the first Gradle dependency download

If Gradle uses the wrong Java version, set `JAVA_HOME` first.

macOS:

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Windows (PowerShell):

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25*"
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
```

## Run the Minecraft client (development)

macOS/Linux:

```bash
./gradlew runClient
```

Windows (PowerShell or cmd):

```bat
gradlew.bat runClient
```

## Build the mod

macOS/Linux:

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

Build outputs are written to `build/libs/`.
