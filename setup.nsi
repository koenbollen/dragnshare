; Installer for HTML Tidy - March 22, 2008

;======================================================
; Includes

  !include MUI.nsh
  !include Sections.nsh
  !include target\project.nsh

;======================================================
; Installer Information

  Name "${PROJECT_NAME}"

  SetCompressor /SOLID lzma
  XPStyle on
  CRCCheck on
  InstallDir "C:\Program Files\${PROJECT_ARTIFACT_ID}\"
  AutoCloseWindow false
  ShowInstDetails show
  Icon "${PROJECT_BASEDIR}\Dragon Ball.ico"

;======================================================
; Version Tab information for Setup.exe properties

  VIProductVersion 0.1.0.0
  VIAddVersionKey ProductName "${PROJECT_NAME}"
  VIAddVersionKey ProductVersion "${PROJECT_VERSION}"
  VIAddVersionKey CompanyName "Koen Bollen & Nils Dijk"
  VIAddVersionKey FileVersion "${PROJECT_VERSION}"
  VIAddVersionKey FileDescription ""
  VIAddVersionKey LegalCopyright ""

;======================================================
; Variables


;======================================================
; Modern Interface Configuration

  !define MUI_HEADERIMAGE
  !define MUI_ABORTWARNING
  !define MUI_COMPONENTSPAGE_SMALLDESC
  !define MUI_HEADERIMAGE_BITMAP_NOSTRETCH
  !define MUI_FINISHPAGE
  !define MUI_FINISHPAGE_TEXT "Thank you for installing ${PROJECT_NAME}."
  !define MUI_ICON "${PROJECT_BASEDIR}\Dragon Ball.ico"

;======================================================
; Modern Interface Pages

  !define MUI_DIRECTORYPAGE_VERIFYONLEAVE
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH

;======================================================
; Languages

  !insertmacro MUI_LANGUAGE "English"

;======================================================
; Installer Sections

Section "Drag'n Share"
    SetOutPath $INSTDIR
    SetOverwrite on
    File target\dragnshare.exe
    File target\dragnshare-0.1-beta.jar

	createShortCut "$SMPROGRAMS\Drag'n Share.lnk" "$INSTDIR\dragnshare.exe"

    writeUninstaller "$INSTDIR\dragnshare_uninstall.exe"
SectionEnd

; Installer functions
Function .onInstSuccess

FunctionEnd

Section "uninstall"
  delete "$INSTDIR\dragnshare*.*"
  delete "$INSTDIR\dragnshare_uninstall.exe"
  delete "$SMPROGRAMS\Drag'n Share.lnk"
SectionEnd

Function .onInit
    InitPluginsDir
FunctionEnd