; Script generated for GlobalTennis Installer
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "GlobalTennis"
#define MyAppPublisher "Xtreme"
#define MyAppExeName "GlobalTennis.exe"
#define MyAppJarName "GlobalTennis_*.jar"

; Default version if not provided via command line
#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{A1B2C3D4-E5F6-7890-1234-56789ABCDEF0}
AppName={#MyAppName}
AppVersion={#AppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
LicenseFile=
; InfoBeforeFile=
; InfoAfterFile=
OutputDir=Output
OutputBaseFilename=GlobalTennis_Setup_v{#AppVersion}
SetupIconFile=
Compression=lzma
SolidCompression=yes
WizardStyle=modern
; Images for the installer (using resources if available)
; WizardImageFile=installer_resources\WixUI_Bmp_Dialog.jpg
; WizardSmallImageFile=installer_resources\WixUI_Bmp_Banner.jpg

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked; OnlyBelowVersion: 6.1; Check: not IsAdminInstallMode

[Files]
; The main JAR file - using wildcard to catch the versioned jar AND renaming it to a fixed name
Source: "dist\{#MyAppJarName}"; DestDir: "{app}"; DestName: "GlobalTennis.jar"; Flags: ignoreversion
; The lib folder dependencies
Source: "dist\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
; Add other resources if needed
; Source: "installer_resources\*"; DestDir: "{app}\resources"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; We need a way to launch the JAR. Usually this involves a batch file or a wrapper exe.
; Since we don't have a wrapper exe ready, we'll create a shortcut to javaw.exe
; NOTE: This assumes Java is installed on the client machine.
Name: "{group}\{#MyAppName}"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\GlobalTennis.jar"""; WorkingDir: "{app}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\GlobalTennis.jar"""; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "javaw.exe"; Parameters: "-jar ""{app}\GlobalTennis.jar"""; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
