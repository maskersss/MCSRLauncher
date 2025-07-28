#define LauncherName "MCSRLauncher"
#define LauncherUrl "https://mcsrlauncher.github.io/"
#define InstallerVersion "1.0"

[Setup]
AppId={{D6D7A3AE-E143-4EDC-BFE2-42C8C9F34CEB}}
AppName={#LauncherName}
AppVerName={#LauncherName}
AppPublisher={#LauncherName}
AppPublisherURL={#LauncherUrl}
AppSupportURL={#LauncherUrl}
AppUpdatesURL={#LauncherUrl}
ChangesAssociations=yes
CreateUninstallRegKey=yes
Compression=lzma2/max
DefaultDirName={localappdata}\{#LauncherName}
DefaultGroupName={#LauncherName}
DisableWelcomePage=no
OutputBaseFilename={#LauncherName}-windows-installer
PrivilegesRequired=lowest
SolidCompression=yes
SetupIconFile=..\icons\icon.ico
UninstallDisplayIcon={app}\launch.exe
WizardStyle=modern

[Components]
Name: "launcher"; Description: "{#LauncherName}"; ExtraDiskSpaceRequired: 33554432; Types: full compact custom; Flags: fixed
Name: "java"; Description: "Install Java 17"; ExtraDiskSpaceRequired: 48928520; Types: full

[Files]
Source: "out\launch.exe"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{tmp}\{#LauncherName}.jar"; DestDir: "{app}"; Flags: external ignoreversion recursesubdirs createallsubdirs
Source: "7za.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall; Components: java
Source: "{tmp}\jre.zip"; DestDir: "{tmp}"; Flags: external deleteafterinstall; Components: java

[Run]
Filename: {tmp}\7za.exe; Parameters: "x ""{tmp}\jre.zip"" -o""{app}\"" * -r -aoa"; Flags: runhidden runascurrentuser; Components: java
Filename: {app}\launch.exe; Description: {cm:LaunchProgram,{#LauncherName}}; Flags: nowait postinstall skipifsilent

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce

[Icons]
Name: "{group}\{#LauncherName}"; Filename: "{app}\launch.exe"
Name: "{userdesktop}\{#LauncherName}"; Filename: "{app}\launch.exe"; Tasks: desktopicon
Name: "{group}\{cm:UninstallProgram,{#LauncherName}}"; Filename: "{uninstallexe}"

[InstallDelete]
Type: filesandordirs; Name: "{app}\jre"; Components: java

[UninstallDelete]
Type: filesandordirs; Name: "{app}\jre"; Components: java

[Code]
#include "lib/JsonHelpers.pas"

const
  LAUNCHER_ENDPOINT = 'https://api.github.com/repos/MCSRLauncher/launcher/releases/latest';
  JRE_META_ENDPOINT = 'https://mcsrlauncher.github.io/meta/net.adoptium.java/java17.json';
  
const
  LAUNCHER_FALLBACK_URL = 'https://github.com/MCSRLauncher/Launcher/releases/download/0.1-beta/MCSRLauncher.jar';
  LAUNCHER_FALLBACK_HASH = '8284bf6b264e9aa6c186b568fc407de459a56ee5593b4b0ef389de0ff265332c';
  JRE_FALLBACK_URL = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jre_x64_windows_hotspot_17.0.16_8.zip';
  JRE_FALLBACK_HASH = 'd35b05f4832215d8877d0dbf15c6370c854d7d5b812f890a9c0db8ad412a6bf2';

var
  DownloadPage: TDownloadWizardPage;
  FallbackUrl, FallbackHash, Url, Hash, Folder: WideString;

procedure GetLauncherInfo;
  var
    WinHttpReq: Variant;
    Json, TempHash: string;
    JsonParser: TJsonParser;
    JsonRoot, AssetObject: TJsonObject;
    AssetsArray: TJsonArray;
begin
  Try
    WinHttpReq := CreateOleObject('WinHttp.WinHttpRequest.5.1');
    WinHttpReq.Open('GET', LAUNCHER_ENDPOINT, False);
    WinHttpReq.Send('');
    if WinHttpReq.Status = 200 then begin
      Json := WinHttpReq.ResponseText;
      if ParseJsonAndLogErrors(JsonParser, Json) then begin
        JsonRoot := GetJsonRoot(JsonParser.Output);
        if FindJsonArray(JsonParser.Output, JsonRoot, 'assets', AssetsArray) then begin
          AssetObject := JsonParser.Output.Objects[AssetsArray[0].Index];
        end
        else begin
          RaiseException('Failed to read Launcher data from GitHub API, falling back to defaults');
        end;

        if not FindJsonString(JsonParser.Output, AssetObject, 'browser_download_url', Url) or
        not FindJsonString(JsonParser.Output, AssetObject, 'digest', Hash) then begin
          RaiseException('Failed to read Launcher data from GitHub API, falling back to defaults');
        end
        else begin
          TempHash := string(Hash);
          StringChangeEx(TempHash, 'sha256:', '', True);
          Hash := WideString(TempHash);
        end;
      end;
      ClearJsonParser(JsonParser);
    end
    else begin
      RaiseException('Failed to read Launcher data from GitHub API, falling back to defaults');
    end;
  Except
    MsgBox(GetExceptionMessage,mbError,MB_OK);
    Url := LAUNCHER_FALLBACK_URL;
    Hash := LAUNCHER_FALLBACK_HASH;
  end;
end;

procedure GetJreInfo;
  var
    WinHttpReq: Variant;
    I: Integer;
    Json: string;
    OS, TargetOS: WideString;
    JsonParser: TJsonParser;
    JsonRoot, JreObject, FinalJreObject, ChecksumObject, VersionObject: TJsonObject;
    RuntimesArray: TJsonArray;
    JreVersion: TJsonNumber;
begin
  if IsWin64 then begin
    OS := 'windows-x64';
    FallbackUrl := JRE_FALLBACK_URL;
    FallbackHash := JRE_FALLBACK_HASH;
  end
  else begin
    RaiseException('x86 doesn''t support')
  end;
  Try
    WinHttpReq := CreateOleObject('WinHttp.WinHttpRequest.5.1');
    WinHttpReq.Open('GET', JRE_META_ENDPOINT, False);
    WinHttpReq.Send('');
    if WinHttpReq.Status = 200 then begin
      Json := WinHttpReq.ResponseText;;
      if ParseJsonAndLogErrors(JsonParser, Json) then begin
        JsonRoot := GetJsonRoot(JsonParser.Output);
        if FindJsonArray(JsonParser.Output, JsonRoot, 'runtimes', RuntimesArray) then begin
          for I := 0 to Length(RuntimesArray) - 1 do begin
            JreObject := JsonParser.Output.Objects[RuntimesArray[I].Index];
            if FindJsonString(JsonParser.Output, JreObject, 'runtimeOS', TargetOS) and (TargetOS = OS) then begin
              FinalJreObject := JreObject;
              Break;
            end;
          end;
          if Length(FinalJreObject) = 0 then begin
            RaiseException('Not found jre for this OS, falling back to defaults');
          end;
        end
        else begin
          RaiseException('Failed to read the meta, falling back to defaults');
        end;

        if not FindJsonString(JsonParser.Output, FinalJreObject, 'url', Url) or
        not FindJsonObject(JsonParser.Output, FinalJreObject, 'version', VersionObject) or
        not FindJsonObject(JsonParser.Output, FinalJreObject, 'checksum', ChecksumObject) or
        not FindJsonString(JsonParser.Output, ChecksumObject, 'hash', Hash) then begin
          RaiseException('Failed to read the meta, falling back to defaults');
        end
        else begin
          Folder := 'jdk-';
          if FindJsonNumber(JsonParser.Output, VersionObject, 'major', JreVersion) then begin
            Folder := Folder + IntToStr(Round(JreVersion));
          end;
          if FindJsonNumber(JsonParser.Output, VersionObject, 'minor', JreVersion) then begin
            Folder := Folder + '.' + IntToStr(Round(JreVersion));
          end;
          if FindJsonNumber(JsonParser.Output, VersionObject, 'security', JreVersion) then begin
            Folder := Folder + '.'  + IntToStr(Round(JreVersion));
          end;
          if FindJsonNumber(JsonParser.Output, VersionObject, 'patch', JreVersion) then begin
            Folder := Folder + '.'  + IntToStr(Round(JreVersion));
          end;
          if FindJsonNumber(JsonParser.Output, VersionObject, 'build', JreVersion) then begin
            Folder := Folder + '+'  + IntToStr(Round(JreVersion));
          end;
          Folder := Folder + '-jre';
        end;
      end;
      ClearJsonParser(JsonParser);
    end
    else begin
      RaiseException('Failed to read the meta, falling back to defaults');
    end;
  Except
    MsgBox(GetExceptionMessage,mbError,MB_OK);
  end;
end;

procedure InitializeWizard;
begin
  DownloadPage := CreateDownloadPage(SetupMessage(msgWizardPreparing), SetupMessage(msgPreparingDesc), nil);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if (CurStep = ssPostInstall) and WizardIsComponentSelected('java') then begin
    if not RenameFile(ExpandConstant('{app}') + '\' + Folder, ExpandConstant('{app}/jre')) then begin
      MsgBox('Failed to rename jre directory. Please try again', mbError, MB_OK);
      WizardForm.Repaint;
    end
  end
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  Retry: Boolean;
  Answer: Integer;
begin
  if CurPageID = wpReady then begin
    try
      // The launcher download must complete
      repeat
        DownloadPage.Clear;
        GetLauncherInfo;
        DownloadPage.Add(Url, '{#LauncherName}.jar', Hash);
        DownloadPage.Show;
        try
          DownloadPage.Download;
          Result := True;
          Retry := False;
        except
          Answer := SuppressibleMsgBox(AddPeriod(GetExceptionMessage), mbCriticalError, MB_RETRYCANCEL, IDRETRY);
          Retry := (Answer = IDRETRY);
          Result := (Answer <> IDCANCEL);
        end;
      until not Retry;

      if not Result then Exit;
      if not WizardIsComponentSelected('java') then Exit;

      // Now do the download for the JRE, but make it optional and okay if it fails
      repeat
        DownloadPage.Clear;
        GetJreInfo;
        DownloadPage.Add(Url, 'jre.zip', Hash);
        try
          DownloadPage.Download;
          Result := True;
          Retry := False;
        except
          Answer := SuppressibleMsgBox(AddPeriod(GetExceptionMessage), mbCriticalError, MB_ABORTRETRYIGNORE, IDIGNORE);
          Retry := (Answer = IDRETRY);
          Result := (Answer <> IDABORT);
        end;
      until not Retry;
    finally
      DownloadPage.Hide;
    end;
  end else
    Result := True;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
  begin
    if MsgBox('Do you want to delete all the launchers data (instances, assets, libraries, etc)?', mbConfirmation, MB_YESNO) = IDYES then begin
      if DelTree(ExpandConstant('{app}/'), True, True, True) then
      begin
      end else
      begin
        MsgBox('Error deleting user data. Please delete it manually.', mbError, MB_OK);
      end;
    end;
  end;
end;