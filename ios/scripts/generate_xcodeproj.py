#!/usr/bin/env python3
from pathlib import Path
import hashlib

root = Path(__file__).resolve().parents[2]
ios_root = root / 'ios'
src_root = ios_root / 'SchoolHelperIOS'
proj_dir = ios_root / 'SchoolHelperIOS.xcodeproj'
workspace_dir = proj_dir / 'project.xcworkspace'
scheme_dir = proj_dir / 'xcshareddata' / 'xcschemes'
resources_dir = src_root / 'Resources' / 'Assets.xcassets'
resources_dir.mkdir(parents=True, exist_ok=True)
(resources_dir / 'Contents.json').write_text('''{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
''')
workspace_dir.mkdir(parents=True, exist_ok=True)
scheme_dir.mkdir(parents=True, exist_ok=True)
(workspace_dir / 'contents.xcworkspacedata').write_text('''<?xml version="1.0" encoding="UTF-8"?>
<Workspace version="1.0">
  <FileRef location="self:"/>
</Workspace>
''')

swift_files = sorted([p for p in src_root.rglob('*.swift') if p.is_file()])
resource_files = [resources_dir]

def xid(name: str) -> str:
    return hashlib.md5(name.encode()).hexdigest().upper()[:24]

project_id = xid('project')
root_group_id = xid('root_group')
products_group_id = xid('products_group')
app_group_id = xid('app_group')
core_group_id = xid('core_group')
features_group_id = xid('features_group')
resources_group_id = xid('resources_group')
product_ref_id = xid('product_ref')
app_target_id = xid('app_target')
project_config_list_id = xid('project_config_list')
target_config_list_id = xid('target_config_list')
sources_phase_id = xid('sources_phase')
frameworks_phase_id = xid('frameworks_phase')
resources_phase_id = xid('resources_phase')
project_debug_config_id = xid('project_debug_config')
project_release_config_id = xid('project_release_config')
target_debug_config_id = xid('target_debug_config')
target_release_config_id = xid('target_release_config')

core_subgroups = {name: xid(f'group_Core_{name}') for name in ['Models', 'Networking', 'Storage']}
feature_subgroups = {name: xid(f'group_Features_{name}') for name in ['Home', 'Meals', 'Schedule', 'Settings', 'Setup', 'Timer', 'Timetable']}

file_ref_ids = {}
build_file_ids = {}
for path in swift_files:
    rel = path.relative_to(src_root).as_posix()
    file_ref_ids[rel] = xid('fileref:' + rel)
    build_file_ids[rel] = xid('buildfile:' + rel)
resource_ref_ids = {}
resource_build_ids = {}
for path in resource_files:
    rel = path.relative_to(src_root).as_posix()
    resource_ref_ids[rel] = xid('resref:' + rel)
    resource_build_ids[rel] = xid('resbuild:' + rel)

objects: list[str] = []
def add(line: str = '') -> None:
    objects.append(line)

app_files = [p.relative_to(src_root).as_posix() for p in swift_files if p.parts[len(src_root.parts)] == 'App']
core_files = {k: [] for k in core_subgroups}
for p in swift_files:
    parts = p.relative_to(src_root).parts
    if parts[0] == 'Core' and len(parts) > 1:
        core_files[parts[1]].append('/'.join(parts))
feature_files = {k: [] for k in feature_subgroups}
for p in swift_files:
    parts = p.relative_to(src_root).parts
    if parts[0] == 'Features' and len(parts) > 1:
        feature_files[parts[1]].append('/'.join(parts))

add('// !$*UTF8*$!')
add('{')
add('\tarchiveVersion = 1;')
add('\tclasses = {};')
add('\tobjectVersion = 56;')
add('\tobjects = {')

for rel, fid in file_ref_ids.items():
    name = Path(rel).name
    add(f'\t\t{fid} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "{name}"; sourceTree = "<group>"; }};')
for rel, fid in resource_ref_ids.items():
    name = Path(rel).name
    add(f'\t\t{fid} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = "{name}"; sourceTree = "<group>"; }};')
add(f'\t\t{product_ref_id} /* SchoolHelperIOS.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; path = SchoolHelperIOS.app; sourceTree = BUILT_PRODUCTS_DIR; }};')

for rel, bid in build_file_ids.items():
    name = Path(rel).name
    add(f'\t\t{bid} /* {name} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_ref_ids[rel]} /* {name} */; }};')
for rel, bid in resource_build_ids.items():
    name = Path(rel).name
    add(f'\t\t{bid} /* {name} in Resources */ = {{isa = PBXBuildFile; fileRef = {resource_ref_ids[rel]} /* {name} */; }};')

add(f'\t\t{root_group_id} = {{isa = PBXGroup; children = ({app_group_id} /* App */, {core_group_id} /* Core */, {features_group_id} /* Features */, {resources_group_id} /* Resources */, {products_group_id} /* Products */); sourceTree = "<group>"; }};')
add(f'\t\t{products_group_id} /* Products */ = {{isa = PBXGroup; children = ({product_ref_id} /* SchoolHelperIOS.app */); name = Products; sourceTree = "<group>"; }};')
app_children = ', '.join(file_ref_ids[r] + f' /* {Path(r).name} */' for r in app_files)
add(f'\t\t{app_group_id} /* App */ = {{isa = PBXGroup; children = ({app_children}); path = App; sourceTree = "<group>"; }};')
add(f'\t\t{core_group_id} /* Core */ = {{isa = PBXGroup; children = ({", ".join(core_subgroups[k] + f" /* {k} */" for k in core_subgroups)}); path = Core; sourceTree = "<group>"; }};')
for key, gid in core_subgroups.items():
    children = ', '.join(file_ref_ids[r] + f' /* {Path(r).name} */' for r in core_files[key])
    add(f'\t\t{gid} /* {key} */ = {{isa = PBXGroup; children = ({children}); path = {key}; sourceTree = "<group>"; }};')
add(f'\t\t{features_group_id} /* Features */ = {{isa = PBXGroup; children = ({", ".join(feature_subgroups[k] + f" /* {k} */" for k in feature_subgroups)}); path = Features; sourceTree = "<group>"; }};')
for key, gid in feature_subgroups.items():
    children = ', '.join(file_ref_ids[r] + f' /* {Path(r).name} */' for r in feature_files[key])
    add(f'\t\t{gid} /* {key} */ = {{isa = PBXGroup; children = ({children}); path = {key}; sourceTree = "<group>"; }};')
resource_children = ', '.join(resource_ref_ids[r] + f' /* {Path(r).name} */' for r in resource_ref_ids)
add(f'\t\t{resources_group_id} /* Resources */ = {{isa = PBXGroup; children = ({resource_children}); path = Resources; sourceTree = "<group>"; }};')

sources_children = ', '.join(build_file_ids[r] + f' /* {Path(r).name} in Sources */' for r in file_ref_ids)
add(f'\t\t{sources_phase_id} /* Sources */ = {{isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({sources_children}); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{frameworks_phase_id} /* Frameworks */ = {{isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};')
res_build_children = ', '.join(resource_build_ids[r] + f' /* {Path(r).name} in Resources */' for r in resource_ref_ids)
add(f'\t\t{resources_phase_id} /* Resources */ = {{isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = ({res_build_children}); runOnlyForDeploymentPostprocessing = 0; }};')

project_build_settings = '{ CLANG_ENABLE_MODULES = YES; SWIFT_VERSION = 5.0; }'
add(f'\t\t{project_debug_config_id} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {project_build_settings}; name = Debug; }};')
add(f'\t\t{project_release_config_id} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {project_build_settings}; name = Release; }};')

target_settings = {
    'ASSETCATALOG_COMPILER_APPICON_NAME': '""',
    'CODE_SIGN_STYLE': 'Automatic',
    'CODE_SIGNING_ALLOWED': 'NO',
    'CODE_SIGNING_REQUIRED': 'NO',
    'CURRENT_PROJECT_VERSION': '1',
    'DEVELOPMENT_TEAM': '""',
    'GENERATE_INFOPLIST_FILE': 'YES',
    'INFOPLIST_KEY_CFBundleDisplayName': 'SchoolHelperIOS',
    'INFOPLIST_KEY_UIApplicationSceneManifest_Generation': 'YES',
    'INFOPLIST_KEY_UILaunchScreen_Generation': 'YES',
    'INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone': 'UIInterfaceOrientationPortrait',
    'IPHONEOS_DEPLOYMENT_TARGET': '17.0',
    'LD_RUNPATH_SEARCH_PATHS': '("$(inherited)", "@executable_path/Frameworks")',
    'MARKETING_VERSION': '1.0',
    'PRODUCT_BUNDLE_IDENTIFIER': 'com.leebyungsun.schoolhelperios',
    'PRODUCT_NAME': '"$(TARGET_NAME)"',
    'SDKROOT': 'iphoneos',
    'SUPPORTED_PLATFORMS': '"iphoneos iphonesimulator"',
    'SWIFT_EMIT_LOC_STRINGS': 'NO',
    'SWIFT_VERSION': '5.0',
    'TARGETED_DEVICE_FAMILY': '1',
}
settings_str = '{ ' + ' '.join(f'{k} = {v};' for k, v in target_settings.items()) + ' }'
add(f'\t\t{target_debug_config_id} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {settings_str}; name = Debug; }};')
add(f'\t\t{target_release_config_id} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {settings_str}; name = Release; }};')
add(f'\t\t{project_config_list_id} = {{isa = XCConfigurationList; buildConfigurations = ({project_debug_config_id} /* Debug */, {project_release_config_id} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};')
add(f'\t\t{target_config_list_id} = {{isa = XCConfigurationList; buildConfigurations = ({target_debug_config_id} /* Debug */, {target_release_config_id} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};')
add(f'\t\t{app_target_id} /* SchoolHelperIOS */ = {{isa = PBXNativeTarget; buildConfigurationList = {target_config_list_id}; buildPhases = ({sources_phase_id} /* Sources */, {frameworks_phase_id} /* Frameworks */, {resources_phase_id} /* Resources */); buildRules = (); dependencies = (); name = SchoolHelperIOS; productName = SchoolHelperIOS; productReference = {product_ref_id} /* SchoolHelperIOS.app */; productType = "com.apple.product-type.application"; }};')
add(f'\t\t{project_id} /* Project object */ = {{isa = PBXProject; attributes = {{ LastUpgradeCheck = 1620; TargetAttributes = {{ {app_target_id} = {{ CreatedOnToolsVersion = 16.2; }}; }}; }}; buildConfigurationList = {project_config_list_id}; compatibilityVersion = "Xcode 15.0"; developmentRegion = en; hasScannedForEncodings = 0; knownRegions = (en, Base); mainGroup = {root_group_id}; productRefGroup = {products_group_id}; projectDirPath = ""; projectRoot = ""; targets = ({app_target_id} /* SchoolHelperIOS */); }};')
add('\t};')
add(f'\trootObject = {project_id} /* Project object */;')
add('}')

(proj_dir / 'project.pbxproj').write_text('\n'.join(objects) + '\n')

scheme = f'''<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="1620" version="1.7">
  <BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES">
    <BuildActionEntries>
      <BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">
        <BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{app_target_id}" BuildableName="SchoolHelperIOS.app" BlueprintName="SchoolHelperIOS" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/>
      </BuildActionEntry>
    </BuildActionEntries>
  </BuildAction>
  <TestAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv="YES"><Testables/></TestAction>
  <LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" debugServiceExtension="internal" allowLocationSimulation="YES"><BuildableProductRunnable runnableDebuggingMode="0"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{app_target_id}" BuildableName="SchoolHelperIOS.app" BlueprintName="SchoolHelperIOS" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/></BuildableProductRunnable></LaunchAction>
  <ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES"><BuildableProductRunnable runnableDebuggingMode="0"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{app_target_id}" BuildableName="SchoolHelperIOS.app" BlueprintName="SchoolHelperIOS" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/></BuildableProductRunnable></ProfileAction>
  <AnalyzeAction buildConfiguration="Debug"/>
  <ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES"/>
</Scheme>
'''
(scheme_dir / 'SchoolHelperIOS.xcscheme').write_text(scheme)
print('regenerated', proj_dir)
