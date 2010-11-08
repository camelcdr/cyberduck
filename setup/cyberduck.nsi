; Script generated by the HM NIS Edit Script Wizard.
!include WordFunc.nsh
!insertmacro VersionCompare
!include LogicLib.nsh
!include WinVer.nsh
!include x64.nsh
!include FileAssociation.nsh

!define PRODUCT_NAME "Cyberduck"
!define PRODUCT_WEB_SITE "http://cyberduck.ch"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\Cyberduck.exe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"

SetCompressor /SOLID lzma
RequestExecutionLevel admin

; MUI 1.67 compatible ------
!include "MUI.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "..\cyberduck-application.ico"
;!define MUI_UNICON "..\cyberduck-application.ico"

!define MUI_WELCOMEFINISHPAGE_BITMAP "welcome.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "welcome.bmp"
;!define MUI_HEADERIMAGE_BITMAP "header.bmp"

; Language Selection Dialog Settings
!define MUI_LANGDLL_REGISTRY_ROOT "${PRODUCT_UNINST_ROOT_KEY}"
!define MUI_LANGDLL_REGISTRY_KEY "${PRODUCT_UNINST_KEY}"
!define MUI_LANGDLL_REGISTRY_VALUENAME "NSIS:Language"

;Required .NET framework
!define MIN_FRA_MAJOR "4"
!define MIN_FRA_MINOR "0"
!define MIN_FRA_BUILD "*"

; Welcome page
!define MUI_WELCOMEPAGE_TITLE_3LINES
!insertmacro MUI_PAGE_WELCOME
; License page
;!insertmacro MUI_PAGE_LICENSE "C:\Users\Public\Documents\test.rtf"
; Directory page
!insertmacro MUI_PAGE_DIRECTORY
; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!define MUI_FINISHPAGE_RUN "$INSTDIR\Cyberduck.exe"
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files, default language is English
!insertmacro MUI_LANGUAGE "English"

!insertmacro MUI_LANGUAGE "Catalan"
!insertmacro MUI_LANGUAGE "Czech"
!insertmacro MUI_LANGUAGE "Danish"
!insertmacro MUI_LANGUAGE "Dutch"
!insertmacro MUI_LANGUAGE "Finnish"
!insertmacro MUI_LANGUAGE "French"
!insertmacro MUI_LANGUAGE "German"
!insertmacro MUI_LANGUAGE "Greek"
!insertmacro MUI_LANGUAGE "Hebrew"
!insertmacro MUI_LANGUAGE "Hungarian"
!insertmacro MUI_LANGUAGE "Indonesian"
!insertmacro MUI_LANGUAGE "Italian"
!insertmacro MUI_LANGUAGE "Japanese"
!insertmacro MUI_LANGUAGE "Korean"
!insertmacro MUI_LANGUAGE "Latvian"
!insertmacro MUI_LANGUAGE "Norwegian"
!insertmacro MUI_LANGUAGE "Polish"
!insertmacro MUI_LANGUAGE "Portuguese"
!insertmacro MUI_LANGUAGE "PortugueseBR"
!insertmacro MUI_LANGUAGE "Romanian"
!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "SerbianLatin"
!insertmacro MUI_LANGUAGE "SimpChinese"
!insertmacro MUI_LANGUAGE "Slovak"
!insertmacro MUI_LANGUAGE "Slovenian"
!insertmacro MUI_LANGUAGE "Spanish"
!insertmacro MUI_LANGUAGE "Swedish"
!insertmacro MUI_LANGUAGE "Thai"
!insertmacro MUI_LANGUAGE "TradChinese"
!insertmacro MUI_LANGUAGE "Turkish"
!insertmacro MUI_LANGUAGE "Ukrainian"
!insertmacro MUI_LANGUAGE "Welsh"
; Reserve files
!insertmacro MUI_RESERVEFILE_INSTALLOPTIONS

; MUI end ------

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "${SETUPFILE}"
InstallDir "$PROGRAMFILES\Cyberduck"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show
BrandingText " "

Var InstallDotNET
Var DownloadLink
Var DotNetDesc
Var TargetFilename

Function .onInit
        System::Call 'kernel32::CreateMutexA(i 0, i 0, t "CyberduckMutex") i .r1 ?e'
        Pop $R0
        StrCmp $R0 0 +2
        Abort

        ;!insertmacro MUI_LANGDLL_DISPLAY
        StrCpy $InstallDotNET "No"

        Call CheckFramework
        StrCmp $0 "1" +2
        StrCpy $InstallDotNET "Yes"
        
FunctionEnd

Function .onInstSuccess
         ExecWait "$InstDir\Install.exe"
FunctionEnd

; The "" makes the section hidden.
Section "" SecUninstallPrevious

    Call UninstallPrevious

SectionEnd

Function UninstallPrevious

    ; Check for uninstaller.
    ReadRegStr $R0 HKLM "${PRODUCT_UNINST_KEY}" "UninstallString"

    ${If} $R0 == ""
        Goto Done
    ${EndIf}

    DetailPrint "Removing previous installation."

    ; Run the uninstaller silently.
    ExecWait '"$R0" /S _?=$INSTDIR'

    Done:

FunctionEnd

Section "MainSection" SEC01
  SetOutPath "$INSTDIR"
  SetOverwrite ifnewer
  SetShellVarContext all

  ; Get .NET if required
  ${If} $InstallDotNET == "Yes"
     SetDetailsView hide

     StrCpy $DotNetDesc ".NET Framework 4.0 Client Profile"

     ; differentiate between x86 and x64
     ${If} ${RunningX64}
           StrCpy $DownloadLink "http://download.microsoft.com/download/5/6/2/562A10F9-C9F4-4313-A044-9C94E0A8FAC8/dotNetFx40_Client_x86_x64.exe"
           StrCpy $TargetFilename "dotNetFx40_Client_x86_x64.exe"
     ${Else}
           StrCpy $DownloadLink "http://download.microsoft.com/download/3/1/8/318161B8-9874-48E4-BB38-9EB82C5D6358/dotNetFx40_Client_x86.exe"
           StrCpy $TargetFilename "dotNetFx40_Client_x86.exe"
     ${EndIf}

     inetc::get /NOCANCEL $DownloadLink "$INSTDIR\$TargetFilename" /END
     Pop $1
     ${If} $1 != "OK"
           Delete "$INSTDIR\$TargetFilename"
           Abort "Error while downloading $DotNetDesc."
     ${EndIf}

     ClearErrors ;Make sure there isn't any previous errors.
     DetailPrint "Installing $DotNetDesc"
     SetDetailsPrint none

     ExecWait '"$INSTDIR\$TargetFilename" /passive /showfinalerror' $0
     Delete "$INSTDIR\$TargetFilename"

     ; check return code, see http://msdn.microsoft.com/library/ee942965%28v=VS.100%29.aspx#return_codes
     IntCmp $0 0 NoError
     IntCmp $0 1614 NoError
     IntCmp $0 3010 NoError
     Abort "Error $0 during installation of $DotNetDesc."
     
     NoError:
             SetDetailsPrint both
             SetDetailsView show
  ${EndIf}

  File "${BASEDIR}\Cyberduck.exe"
  File "${BASEDIR}\Cyberduck.exe.config"
  File "${BASEDIR}\Acknowledgments.rtf"
  File "${BASEDIR}\..\..\en.lproj\License.txt"  
  File "${BASEDIR}\cyberduck-document.ico"
  File "${BASEDIR}\*.dll"
  File "${BASEDIR}\..\update\wyUpdate.exe"
  File "${BASEDIR}\..\update\*.wyc"
  
  Push "v4.0"
  Call GetDotNetDir
  Pop $R0 ; .net framework v4.0 installation directory
  StrCmp "" $R0 +3 +1

  DetailPrint "Creating native images"
  nsExec::Exec '"$R0\ngen.exe" install "$INSTDIR\Cyberduck.exe"'

  ; creating file associations
  ${registerExtension} "$INSTDIR\Cyberduck.exe" ".cyberducklicense" "Cyberduck Donation Key" "$INSTDIR\cyberduck-document.ico"
  ${registerExtension} "$INSTDIR\Cyberduck.exe" ".duck" "Cyberduck Bookmark" "$INSTDIR\cyberduck-document.ico"
  ; notify the system that file associations have been changed
  System::Call 'shell32.dll::SHChangeNotify(i, i, i, i) v (0x08000000, 0, 0, 0)'

  CreateDirectory "$SMPROGRAMS\Cyberduck"
  CreateShortCut "$SMPROGRAMS\Cyberduck\Cyberduck.lnk" "$INSTDIR\Cyberduck.exe"
  CreateShortCut "$DESKTOP\Cyberduck.lnk" "$INSTDIR\Cyberduck.exe"
SectionEnd

Section -AdditionalIcons
  WriteIniStr "$INSTDIR\${PRODUCT_NAME}.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
  CreateShortCut "$SMPROGRAMS\Cyberduck\Website.lnk" "$INSTDIR\${PRODUCT_NAME}.url"
  ;CreateShortCut "$SMPROGRAMS\Cyberduck\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\Cyberduck.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\Cyberduck.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
SectionEnd

Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed from your computer." /SD IDOK
FunctionEnd

Function un.onInit
  !insertmacro MUI_UNGETLANGUAGE
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove $(^Name) and all of its components?" /SD IDYES IDYES +2
  Abort
FunctionEnd

Section Uninstall
  SetShellVarContext all

  ;Delete "$SMPROGRAMS\Cyberduck\Uninstall.lnk"
  Delete "$SMPROGRAMS\Cyberduck\Website.lnk"
  Delete "$DESKTOP\Cyberduck.lnk"
  Delete "$SMPROGRAMS\Cyberduck\Cyberduck.lnk"

  RMDir "$SMPROGRAMS\Cyberduck"
  RMDir /r "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  SetAutoClose true
SectionEnd

;Check for .NET framework
Function CheckFrameWork

   ;Save the variables in case something else is using them
  Push $0
  Push $1
  Push $2
  Push $3
  Push $4
  Push $R1
  Push $R2
  Push $R3
  Push $R4
  Push $R5
  Push $R6
  Push $R7
  Push $R8

  StrCpy $R5 "0"
  StrCpy $R6 "0"
  StrCpy $R7 "0"
  StrCpy $R8 "0.0.0"
  StrCpy $0 0

  loop:

  ;Get each sub key under "SOFTWARE\Microsoft\NET Framework Setup\NDP"
  EnumRegKey $1 HKLM "SOFTWARE\Microsoft\NET Framework Setup\NDP" $0
  StrCmp $1 "" done ;jump to end if no more registry keys
  IntOp $0 $0 + 1
  StrCpy $2 $1 1 ;Cut off the first character
  StrCpy $3 $1 "" 1 ;Remainder of string

  ;Loop if first character is not a 'v'
  StrCmpS $2 "v" start_parse loop

  ;Parse the string
  start_parse:
  StrCpy $R1 ""
  StrCpy $R2 ""
  StrCpy $R3 ""
  StrCpy $R4 $3

  StrCpy $4 1

  parse:
  StrCmp $3 "" parse_done ;If string is empty, we are finished
  StrCpy $2 $3 1 ;Cut off the first character
  StrCpy $3 $3 "" 1 ;Remainder of string
  StrCmp $2 "." is_dot not_dot ;Move to next part if it's a dot

  is_dot:
  IntOp $4 $4 + 1 ; Move to the next section
  goto parse ;Carry on parsing

  not_dot:
  IntCmp $4 1 major_ver
  IntCmp $4 2 minor_ver
  IntCmp $4 3 build_ver
  IntCmp $4 4 parse_done

  major_ver:
  StrCpy $R1 $R1$2
  goto parse ;Carry on parsing

  minor_ver:
  StrCpy $R2 $R2$2
  goto parse ;Carry on parsing

  build_ver:
  StrCpy $R3 $R3$2
  goto parse ;Carry on parsing

  parse_done:

  IntCmp $R1 $R5 this_major_same loop this_major_more
  this_major_more:
  StrCpy $R5 $R1
  StrCpy $R6 $R2
  StrCpy $R7 $R3
  StrCpy $R8 $R4

  goto loop

  this_major_same:
  IntCmp $R2 $R6 this_minor_same loop this_minor_more
  this_minor_more:
  StrCpy $R6 $R2
  StrCpy $R7 R3
  StrCpy $R8 $R4
  goto loop

  this_minor_same:
  IntCmp $R3 $R7 loop loop this_build_more
  this_build_more:
  StrCpy $R7 $R3
  StrCpy $R8 $R4
  goto loop

  done:

  ;Have we got the framework we need?
  IntCmp $R5 ${MIN_FRA_MAJOR} max_major_same fail OK
  max_major_same:
  IntCmp $R6 ${MIN_FRA_MINOR} max_minor_same fail OK
  max_minor_same:
  IntCmp $R7 ${MIN_FRA_BUILD} OK fail OK

  ;Version on machine is greater than what we need
  OK:
     StrCpy $0 "1"
     goto end

  fail:
     StrCpy $0 "0"
  
  end:

  ;Pop the variables we pushed earlier
  Pop $R8
  Pop $R7
  Pop $R6
  Pop $R5
  Pop $R4
  Pop $R3
  Pop $R2
  Pop $R1
  Pop $4
  Pop $3
  Pop $2
  Pop $1
FunctionEnd

; Given a .NET version number, this function returns that .NET framework's
; install directory. Returns "" if the given .NET version is not installed.
; Params: [version] (eg. "v2.0")
; Return: [dir] (eg. "C:\WINNT\Microsoft.NET\Framework\v2.0.50727")
Function GetDotNetDir
	Exch $R0 ; Set R0 to .net version major
	Push $R1
	Push $R2

        ClearErrors

	; set R1 to minor version number of the installed .NET runtime
	EnumRegValue $R1 HKLM "Software\Microsoft\.NetFramework\policy\$R0" 0
	IfErrors getdotnetdir_err

	; set R2 to .NET install dir root
	ReadRegStr $R2 HKLM "Software\Microsoft\.NetFramework" "InstallRoot"
	IfErrors getdotnetdir_err

	; set R0 to the .NET install dir full
	StrCpy $R0 "$R2$R0.$R1"

getdotnetdir_end:
	Pop $R2
	Pop $R1
	Exch $R0 ; return .net install dir full
	Return

getdotnetdir_err:
	StrCpy $R0 ""
	Goto getdotnetdir_end

FunctionEnd
