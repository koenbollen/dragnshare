; Java Launcher with automatic JRE installation
;-----------------------------------------------

!include target\project.nsh

Name "Drag'n Share"
Caption "Drag'n Share"
Icon "${PROJECT_BASEDIR}/Dragon Ball.ico"
 
VIAddVersionKey "ProductName" "Drag'n Share"
VIAddVersionKey "Comments" "A test comment"
VIAddVersionKey "CompanyName" "Koen & Nils"
VIProductVersion "0.1.0.0"
 
!define CLASSPATH "dragnshare-0.1-beta.jar"
!define CLASS "nl.thanod.dragnshare.DropZone"
!define PRODUCT_NAME "Drag'n Share"
 
; Definitions for Java 6.0
!define JRE_VERSION "6.0"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=24936&/jre-6u10-windows-i586-p.exe"
 
!define JAVAEXE "javaw.exe"
 
RequestExecutionLevel user
SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow
 
!include "FileFunc.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare
 
Section ""
  Call GetJRE
  Pop $R0
 
  ; change for your purpose (-jar etc.)
  ${GetParameters} $1
  StrCpy $0 '"$R0" -classpath "${CLASSPATH}" ${CLASS} $1'
 
  SetOutPath $EXEDIR
  Exec $0
SectionEnd
 
;  returns the full path of a valid java.exe
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - JAVA_HOME environment variable
;  3 - the registry
;  4 - hopes it is in current dir or PATH
Function GetJRE
    Push $R0
    Push $R1
    Push $2
 
  ; 1) Check local JRE
  CheckLocal:
    ClearErrors
    StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
    IfFileExists $R0 JreFound
 
  ; 2) Check for JAVA_HOME
  CheckJavaHome:
    ClearErrors
    ReadEnvStr $R0 "JAVA_HOME"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfErrors CheckRegistry     
    IfFileExists $R0 0 CheckRegistry
    Call CheckJREVersion
    IfErrors CheckRegistry JreFound
 
  ; 3) Check for registry
  CheckRegistry:
    ClearErrors
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfErrors DownloadJRE
    IfFileExists $R0 0 DownloadJRE
    Call CheckJREVersion
    IfErrors DownloadJRE JreFound
 
  DownloadJRE:
    MessageBox MB_ICONINFORMATION "${PRODUCT_NAME} uses Java Runtime Environment ${JRE_VERSION}, it will now be downloaded and installed."
    StrCpy $2 "$TEMP\Java Runtime Environment.exe"
    nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
    Pop $R0 ;Get the return value
    StrCmp $R0 "success" +3
      MessageBox MB_ICONSTOP "Download failed: $R0"
      Abort
    ExecWait $2
    Delete $2
 
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfFileExists $R0 0 GoodLuck
    Call CheckJREVersion
    IfErrors GoodLuck JreFound
 
  ; 4) wishing you good luck
  GoodLuck:
    StrCpy $R0 "${JAVAEXE}"
    ; MessageBox MB_ICONSTOP "Cannot find appropriate Java Runtime Environment."
    ; Abort
 
  JreFound:
    Pop $2
    Pop $R1
    Exch $R0
FunctionEnd
 
; Pass the "javaw.exe" path by $R0
Function CheckJREVersion
    Push $R1
 
    ; Get the file version of javaw.exe
    ${GetFileVersion} $R0 $R1
    ${VersionCompare} ${JRE_VERSION} $R1 $R1
 
    ; Check whether $R1 != "1"
    ClearErrors
    StrCmp $R1 "1" 0 CheckDone
    SetErrors
 
  CheckDone:
    Pop $R1
FunctionEnd
 