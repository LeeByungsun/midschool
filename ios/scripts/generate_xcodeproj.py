#!/usr/bin/env python3
from pathlib import Path
import hashlib

root = Path(__file__).resolve().parents[2]
ios_root = root / 'ios'
src_root = ios_root / 'SchoolHelperIOS'
widget_root = ios_root / 'SchoolHelperWidget'
tests_root = ios_root / 'SchoolHelperIOSTests'
proj_dir = ios_root / 'SchoolHelperIOS.xcodeproj'
workspace_dir = proj_dir / 'project.xcworkspace'
scheme_dir = proj_dir / 'xcshareddata' / 'xcschemes'
resources_dir = src_root / 'Resources' / 'Assets.xcassets'
resources_dir.mkdir(parents=True, exist_ok=True)
workspace_dir.mkdir(parents=True, exist_ok=True)
scheme_dir.mkdir(parents=True, exist_ok=True)
(resources_dir / 'Contents.json').write_text('''{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
''')
(workspace_dir / 'contents.xcworkspacedata').write_text('''<?xml version="1.0" encoding="UTF-8"?>
<Workspace version="1.0">
  <FileRef location="self:"/>
</Workspace>
''')

swift_files = sorted([p for p in src_root.rglob('*.swift') if p.is_file()])
widget_files = sorted([p for p in widget_root.rglob('*.swift') if p.is_file()]) if widget_root.exists() else []
test_files = sorted([p for p in tests_root.rglob('*.swift') if p.is_file()]) if tests_root.exists() else []
resource_files = [resources_dir]

def xid(name: str) -> str:
    return hashlib.md5(name.encode()).hexdigest().upper()[:24]

ids = {k: xid(k) for k in [
    'project','root_group','source_root_group','widget_root_group','products_group','app_group','core_group','features_group','resources_group','tests_group',
    'product_ref','tests_product_ref','widget_product_ref','app_target','tests_target','widget_target','project_config_list','app_config_list','tests_config_list','widget_config_list',
    'sources_phase','frameworks_phase','resources_phase','embed_appex_phase','tests_sources_phase','tests_frameworks_phase','tests_resources_phase','widget_sources_phase','widget_frameworks_phase','widget_resources_phase',
    'widget_embed_build_file','widget_target_proxy','widget_target_dependency',
    'project_debug','project_release','app_debug','app_release','tests_debug','tests_release','widget_debug','widget_release'
]}
core_subgroups = {name: xid(f'group_Core_{name}') for name in ['Models', 'Networking', 'Notifications', 'Repositories', 'Storage']}
feature_subgroups = {name: xid(f'group_Features_{name}') for name in ['Home', 'Meals', 'Schedule', 'Settings', 'Setup', 'Timer', 'Timetable']}

file_ref_ids = {}
build_file_ids = {}
for path in swift_files:
    rel = path.relative_to(src_root).as_posix()
    file_ref_ids[rel] = xid('fileref:' + rel)
    build_file_ids[rel] = xid('buildfile:' + rel)

test_ref_ids = {}
test_build_ids = {}
for path in test_files:
    rel = path.relative_to(tests_root).as_posix()
    test_ref_ids[rel] = xid('testref:' + rel)
    test_build_ids[rel] = xid('testbuild:' + rel)

resource_ref_ids = {}
resource_build_ids = {}
for path in resource_files:
    rel = path.relative_to(src_root).as_posix()
    resource_ref_ids[rel] = xid('resref:' + rel)
    resource_build_ids[rel] = xid('resbuild:' + rel)

widget_ref_ids = {}
widget_build_ids = {}
for path in widget_files:
    rel = path.relative_to(widget_root).as_posix()
    widget_ref_ids[rel] = xid('widgetref:' + rel)
    widget_build_ids[rel] = xid('widgetbuild:' + rel)

objects=[]
def add(line=''): objects.append(line)

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

test_group_files = [p.relative_to(tests_root).as_posix() for p in test_files]

add('// !$*UTF8*$!')
add('{')
add('\tarchiveVersion = 1;')
add('\tclasses = {};')
add('\tobjectVersion = 56;')
add('\tobjects = {')

for rel, fid in file_ref_ids.items():
    add(f'\t\t{fid} /* {Path(rel).name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "{Path(rel).name}"; sourceTree = "<group>"; }};')
for rel, fid in test_ref_ids.items():
    add(f'\t\t{fid} /* {Path(rel).name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "{Path(rel).name}"; sourceTree = "<group>"; }};')
for rel, fid in widget_ref_ids.items():
    add(f'\t\t{fid} /* {Path(rel).name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "{Path(rel).name}"; sourceTree = "<group>"; }};')
for rel, fid in resource_ref_ids.items():
    add(f'\t\t{fid} /* {Path(rel).name} */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = "{Path(rel).name}"; sourceTree = "<group>"; }};')
add(f'\t\t{ids["product_ref"]} /* SchoolHelperIOS.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; path = SchoolHelperIOS.app; sourceTree = BUILT_PRODUCTS_DIR; }};')
add(f'\t\t{ids["tests_product_ref"]} /* SchoolHelperIOSTests.xctest */ = {{isa = PBXFileReference; explicitFileType = wrapper.cfbundle; path = SchoolHelperIOSTests.xctest; sourceTree = BUILT_PRODUCTS_DIR; }};')
add(f'\t\t{ids["widget_product_ref"]} /* SchoolHelperWidget.appex */ = {{isa = PBXFileReference; explicitFileType = "wrapper.app-extension"; path = SchoolHelperWidget.appex; sourceTree = BUILT_PRODUCTS_DIR; }};')

for rel, bid in build_file_ids.items():
    add(f'\t\t{bid} /* {Path(rel).name} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_ref_ids[rel]} /* {Path(rel).name} */; }};')
for rel, bid in test_build_ids.items():
    add(f'\t\t{bid} /* {Path(rel).name} in Sources */ = {{isa = PBXBuildFile; fileRef = {test_ref_ids[rel]} /* {Path(rel).name} */; }};')
for rel, bid in widget_build_ids.items():
    add(f'\t\t{bid} /* {Path(rel).name} in Sources */ = {{isa = PBXBuildFile; fileRef = {widget_ref_ids[rel]} /* {Path(rel).name} */; }};')
for rel, bid in resource_build_ids.items():
    add(f'\t\t{bid} /* {Path(rel).name} in Resources */ = {{isa = PBXBuildFile; fileRef = {resource_ref_ids[rel]} /* {Path(rel).name} */; }};')
add(f'\t\t{ids["widget_embed_build_file"]} /* SchoolHelperWidget.appex in Embed App Extensions */ = {{isa = PBXBuildFile; fileRef = {ids["widget_product_ref"]} /* SchoolHelperWidget.appex */; settings = {{ATTRIBUTES = (RemoveHeadersOnCopy, ); }}; }};')

add(f'\t\t{ids["root_group"]} = {{isa = PBXGroup; children = ({ids["source_root_group"]} /* SchoolHelperIOS */, {ids["widget_root_group"]} /* SchoolHelperWidget */, {ids["tests_group"]} /* SchoolHelperIOSTests */, {ids["products_group"]} /* Products */); sourceTree = "<group>"; }};')
add(f'\t\t{ids["source_root_group"]} /* SchoolHelperIOS */ = {{isa = PBXGroup; children = ({ids["app_group"]} /* App */, {ids["core_group"]} /* Core */, {ids["features_group"]} /* Features */, {ids["resources_group"]} /* Resources */); path = SchoolHelperIOS; sourceTree = "<group>"; }};')
widget_children = ', '.join(widget_ref_ids[r] + f' /* {Path(r).name} */' for r in widget_ref_ids)
add(f'\t\t{ids["widget_root_group"]} /* SchoolHelperWidget */ = {{isa = PBXGroup; children = ({widget_children}); path = SchoolHelperWidget; sourceTree = "<group>"; }};')
add(f'\t\t{ids["products_group"]} /* Products */ = {{isa = PBXGroup; children = ({ids["product_ref"]} /* SchoolHelperIOS.app */, {ids["tests_product_ref"]} /* SchoolHelperIOSTests.xctest */, {ids["widget_product_ref"]} /* SchoolHelperWidget.appex */); name = Products; sourceTree = "<group>"; }};')
app_children = ', '.join(file_ref_ids[r] + f' /* {Path(r).name} */' for r in app_files)
add(f'\t\t{ids["app_group"]} /* App */ = {{isa = PBXGroup; children = ({app_children}); path = App; sourceTree = "<group>"; }};')
add(f'\t\t{ids["core_group"]} /* Core */ = {{isa = PBXGroup; children = ({", ".join(core_subgroups[k] + f" /* {k} */" for k in core_subgroups)}); path = Core; sourceTree = "<group>"; }};')
for key,gid in core_subgroups.items():
    children = ', '.join(file_ref_ids[r] + f' /* {Path(r).name} */' for r in core_files[key])
    add(f'\t\t{gid} /* {key} */ = {{isa = PBXGroup; children = ({children}); path = {key}; sourceTree = "<group>"; }};')
add(f'\t\t{ids["features_group"]} /* Features */ = {{isa = PBXGroup; children = ({", ".join(feature_subgroups[k] + f" /* {k} */" for k in feature_subgroups)}); path = Features; sourceTree = "<group>"; }};')
for key,gid in feature_subgroups.items():
    children = ', '.join(file_ref_ids[r] + f' /* {Path(r).name} */' for r in feature_files[key])
    add(f'\t\t{gid} /* {key} */ = {{isa = PBXGroup; children = ({children}); path = {key}; sourceTree = "<group>"; }};')
res_children = ', '.join(resource_ref_ids[r] + f' /* {Path(r).name} */' for r in resource_ref_ids)
add(f'\t\t{ids["resources_group"]} /* Resources */ = {{isa = PBXGroup; children = ({res_children}); path = Resources; sourceTree = "<group>"; }};')
test_children = ', '.join(test_ref_ids[r] + f' /* {Path(r).name} */' for r in test_group_files)
add(f'\t\t{ids["tests_group"]} /* SchoolHelperIOSTests */ = {{isa = PBXGroup; children = ({test_children}); path = SchoolHelperIOSTests; sourceTree = "<group>"; }};')

sources_children = ', '.join(build_file_ids[r] + f' /* {Path(r).name} in Sources */' for r in file_ref_ids)
add(f'\t\t{ids["sources_phase"]} /* Sources */ = {{isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({sources_children}); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{ids["frameworks_phase"]} /* Frameworks */ = {{isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};')
res_build_children = ', '.join(resource_build_ids[r] + f' /* {Path(r).name} in Resources */' for r in resource_ref_ids)
add(f'\t\t{ids["resources_phase"]} /* Resources */ = {{isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = ({res_build_children}); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{ids["embed_appex_phase"]} /* Embed App Extensions */ = {{isa = PBXCopyFilesBuildPhase; buildActionMask = 2147483647; dstPath = ""; dstSubfolderSpec = 13; files = ({ids["widget_embed_build_file"]} /* SchoolHelperWidget.appex in Embed App Extensions */); name = "Embed App Extensions"; runOnlyForDeploymentPostprocessing = 0; }};')
test_sources = ', '.join(test_build_ids[r] + f' /* {Path(r).name} in Sources */' for r in test_ref_ids)
add(f'\t\t{ids["tests_sources_phase"]} /* Sources */ = {{isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({test_sources}); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{ids["tests_frameworks_phase"]} /* Frameworks */ = {{isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{ids["tests_resources_phase"]} /* Resources */ = {{isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};')
widget_core_sources = [r for r in file_ref_ids if r.startswith('Core/')]
widget_source_entries = ', '.join(
    [widget_build_ids[r] + f' /* {Path(r).name} in Sources */' for r in widget_ref_ids] +
    [build_file_ids[r] + f' /* {Path(r).name} in Sources */' for r in widget_core_sources]
)
add(f'\t\t{ids["widget_sources_phase"]} /* Sources */ = {{isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({widget_source_entries}); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{ids["widget_frameworks_phase"]} /* Frameworks */ = {{isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};')
add(f'\t\t{ids["widget_resources_phase"]} /* Resources */ = {{isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};')

project_settings = '{ CLANG_ENABLE_MODULES = YES; SWIFT_VERSION = 5.0; }'
add(f'\t\t{ids["project_debug"]} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {project_settings}; name = Debug; }};')
add(f'\t\t{ids["project_release"]} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {project_settings}; name = Release; }};')
app_target_settings = {
    'ASSETCATALOG_COMPILER_APPICON_NAME': '""', 'CODE_SIGN_STYLE': 'Automatic', 'CODE_SIGNING_ALLOWED': 'NO', 'CODE_SIGNING_REQUIRED': 'NO',
    'CODE_SIGN_ENTITLEMENTS': 'SchoolHelperIOS/SchoolHelperIOS.entitlements',
    'CURRENT_PROJECT_VERSION': '1', 'DEVELOPMENT_TEAM': '""', 'ENABLE_TESTABILITY': 'YES', 'GENERATE_INFOPLIST_FILE': 'NO',
    'INFOPLIST_FILE': 'SchoolHelperIOS/Info.plist',
    'IPHONEOS_DEPLOYMENT_TARGET': '17.0', 'LD_RUNPATH_SEARCH_PATHS': '("$(inherited)", "@executable_path/Frameworks")',
    'MARKETING_VERSION': '1.0', 'PRODUCT_BUNDLE_IDENTIFIER': 'com.leebyungsun.schoolhelperios', 'PRODUCT_NAME': '"$(TARGET_NAME)"',
    'SUPPORTED_PLATFORMS': '"iphoneos iphonesimulator"', 'SWIFT_EMIT_LOC_STRINGS': 'NO', 'SWIFT_OPTIMIZATION_LEVEL': '"-Onone"', 'SWIFT_VERSION': '5.0', 'TARGETED_DEVICE_FAMILY': '1'
}
app_settings = '{ ' + ' '.join(f'{k} = {v};' for k,v in app_target_settings.items()) + ' }'
add(f'\t\t{ids["app_debug"]} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {app_settings}; name = Debug; }};')
app_release_settings = app_target_settings | {'ENABLE_TESTABILITY': 'NO', 'SWIFT_OPTIMIZATION_LEVEL': '"-O"'}
app_release = '{ ' + ' '.join(f'{k} = {v};' for k,v in app_release_settings.items()) + ' }'
add(f'\t\t{ids["app_release"]} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {app_release}; name = Release; }};')

test_target_settings = {
    'BUNDLE_LOADER': '"$(TEST_HOST)"', 'CODE_SIGN_STYLE': 'Automatic', 'CODE_SIGNING_ALLOWED': 'NO', 'CODE_SIGNING_REQUIRED': 'NO',
    'ENABLE_TESTABILITY': 'YES',
    'GENERATE_INFOPLIST_FILE': 'YES', 'IPHONEOS_DEPLOYMENT_TARGET': '17.0', 'LD_RUNPATH_SEARCH_PATHS': '("$(inherited)", "@executable_path/Frameworks", "@loader_path/Frameworks")',
    'PRODUCT_BUNDLE_IDENTIFIER': 'com.leebyungsun.schoolhelperios.tests', 'PRODUCT_NAME': '"$(TARGET_NAME)"',
    'SUPPORTED_PLATFORMS': '"iphoneos iphonesimulator"', 'SWIFT_OPTIMIZATION_LEVEL': '"-Onone"', 'SWIFT_VERSION': '5.0', 'TARGETED_DEVICE_FAMILY': '1', 'TEST_HOST': '"$(BUILT_PRODUCTS_DIR)/SchoolHelperIOS.app/SchoolHelperIOS"'
}
test_settings = '{ ' + ' '.join(f'{k} = {v};' for k,v in test_target_settings.items()) + ' }'
add(f'\t\t{ids["tests_debug"]} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {test_settings}; name = Debug; }};')
test_release_settings = test_target_settings | {'ENABLE_TESTABILITY': 'NO', 'SWIFT_OPTIMIZATION_LEVEL': '"-O"'}
test_release = '{ ' + ' '.join(f'{k} = {v};' for k,v in test_release_settings.items()) + ' }'
add(f'\t\t{ids["tests_release"]} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {test_release}; name = Release; }};')
widget_target_settings = {
    'APPLICATION_EXTENSION_API_ONLY': 'YES', 'CODE_SIGN_STYLE': 'Automatic', 'CODE_SIGNING_ALLOWED': 'NO', 'CODE_SIGNING_REQUIRED': 'NO',
    'CODE_SIGN_ENTITLEMENTS': 'SchoolHelperWidget/SchoolHelperWidget.entitlements',
    'CURRENT_PROJECT_VERSION': '1', 'DEVELOPMENT_TEAM': '""', 'GENERATE_INFOPLIST_FILE': 'NO',
    'INFOPLIST_FILE': 'SchoolHelperWidget/Info.plist',
    'IPHONEOS_DEPLOYMENT_TARGET': '17.0',
    'LD_RUNPATH_SEARCH_PATHS': '("$(inherited)", "@executable_path/Frameworks", "@executable_path/../../Frameworks")',
    'MARKETING_VERSION': '1.0', 'PRODUCT_BUNDLE_IDENTIFIER': 'com.leebyungsun.schoolhelperios.widget', 'PRODUCT_NAME': '"$(TARGET_NAME)"',
    'SKIP_INSTALL': 'YES', 'SUPPORTED_PLATFORMS': '"iphoneos iphonesimulator"', 'SWIFT_VERSION': '5.0', 'TARGETED_DEVICE_FAMILY': '1'
}
widget_settings = '{ ' + ' '.join(f'{k} = {v};' for k,v in widget_target_settings.items()) + ' }'
add(f'\t\t{ids["widget_debug"]} /* Debug */ = {{isa = XCBuildConfiguration; buildSettings = {widget_settings}; name = Debug; }};')
add(f'\t\t{ids["widget_release"]} /* Release */ = {{isa = XCBuildConfiguration; buildSettings = {widget_settings}; name = Release; }};')
add(f'\t\t{ids["project_config_list"]} = {{isa = XCConfigurationList; buildConfigurations = ({ids["project_debug"]} /* Debug */, {ids["project_release"]} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};')
add(f'\t\t{ids["app_config_list"]} = {{isa = XCConfigurationList; buildConfigurations = ({ids["app_debug"]} /* Debug */, {ids["app_release"]} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};')
add(f'\t\t{ids["tests_config_list"]} = {{isa = XCConfigurationList; buildConfigurations = ({ids["tests_debug"]} /* Debug */, {ids["tests_release"]} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};')
add(f'\t\t{ids["widget_config_list"]} = {{isa = XCConfigurationList; buildConfigurations = ({ids["widget_debug"]} /* Debug */, {ids["widget_release"]} /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};')
add(f'\t\t{ids["widget_target_proxy"]} /* PBXContainerItemProxy */ = {{isa = PBXContainerItemProxy; containerPortal = {ids["project"]} /* Project object */; proxyType = 1; remoteGlobalIDString = {ids["widget_target"]}; remoteInfo = SchoolHelperWidget; }};')
add(f'\t\t{ids["widget_target_dependency"]} /* PBXTargetDependency */ = {{isa = PBXTargetDependency; target = {ids["widget_target"]} /* SchoolHelperWidget */; targetProxy = {ids["widget_target_proxy"]} /* PBXContainerItemProxy */; }};')
add(f'\t\t{ids["app_target"]} /* SchoolHelperIOS */ = {{isa = PBXNativeTarget; buildConfigurationList = {ids["app_config_list"]}; buildPhases = ({ids["sources_phase"]} /* Sources */, {ids["frameworks_phase"]} /* Frameworks */, {ids["resources_phase"]} /* Resources */, {ids["embed_appex_phase"]} /* Embed App Extensions */); buildRules = (); dependencies = ({ids["widget_target_dependency"]} /* PBXTargetDependency */); name = SchoolHelperIOS; productName = SchoolHelperIOS; productReference = {ids["product_ref"]} /* SchoolHelperIOS.app */; productType = "com.apple.product-type.application"; }};')
add(f'\t\t{ids["tests_target"]} /* SchoolHelperIOSTests */ = {{isa = PBXNativeTarget; buildConfigurationList = {ids["tests_config_list"]}; buildPhases = ({ids["tests_sources_phase"]} /* Sources */, {ids["tests_frameworks_phase"]} /* Frameworks */, {ids["tests_resources_phase"]} /* Resources */); buildRules = (); dependencies = (); name = SchoolHelperIOSTests; productName = SchoolHelperIOSTests; productReference = {ids["tests_product_ref"]} /* SchoolHelperIOSTests.xctest */; productType = "com.apple.product-type.bundle.unit-test"; }};')
add(f'\t\t{ids["widget_target"]} /* SchoolHelperWidget */ = {{isa = PBXNativeTarget; buildConfigurationList = {ids["widget_config_list"]}; buildPhases = ({ids["widget_sources_phase"]} /* Sources */, {ids["widget_frameworks_phase"]} /* Frameworks */, {ids["widget_resources_phase"]} /* Resources */); buildRules = (); dependencies = (); name = SchoolHelperWidget; productName = SchoolHelperWidget; productReference = {ids["widget_product_ref"]} /* SchoolHelperWidget.appex */; productType = "com.apple.product-type.app-extension"; }};')
add(f'\t\t{ids["project"]} /* Project object */ = {{isa = PBXProject; attributes = {{ LastUpgradeCheck = 1620; TargetAttributes = {{ {ids["app_target"]} = {{ CreatedOnToolsVersion = 16.2; }}; {ids["tests_target"]} = {{ CreatedOnToolsVersion = 16.2; TestTargetID = {ids["app_target"]}; }}; {ids["widget_target"]} = {{ CreatedOnToolsVersion = 16.2; }}; }}; }}; buildConfigurationList = {ids["project_config_list"]}; compatibilityVersion = "Xcode 15.0"; developmentRegion = en; hasScannedForEncodings = 0; knownRegions = (en, Base); mainGroup = {ids["root_group"]}; productRefGroup = {ids["products_group"]}; projectDirPath = ""; projectRoot = ""; targets = ({ids["app_target"]} /* SchoolHelperIOS */, {ids["tests_target"]} /* SchoolHelperIOSTests */, {ids["widget_target"]} /* SchoolHelperWidget */); }};')
add('\t};')
add(f'\trootObject = {ids["project"]} /* Project object */;')
add('}')

(proj_dir / 'project.pbxproj').write_text('\n'.join(objects) + '\n')

scheme = f'''<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="1620" version="1.7">
  <BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES">
    <BuildActionEntries>
      <BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">
        <BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ids["app_target"]}" BuildableName="SchoolHelperIOS.app" BlueprintName="SchoolHelperIOS" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/>
      </BuildActionEntry>
      <BuildActionEntry buildForTesting="YES" buildForRunning="NO" buildForProfiling="NO" buildForArchiving="NO" buildForAnalyzing="YES">
        <BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ids["tests_target"]}" BuildableName="SchoolHelperIOSTests.xctest" BlueprintName="SchoolHelperIOSTests" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/>
      </BuildActionEntry>
      <BuildActionEntry buildForTesting="NO" buildForRunning="NO" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">
        <BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ids["widget_target"]}" BuildableName="SchoolHelperWidget.appex" BlueprintName="SchoolHelperWidget" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/>
      </BuildActionEntry>
    </BuildActionEntries>
  </BuildAction>
  <TestAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv="YES"><Testables><TestableReference skipped="NO"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ids["tests_target"]}" BuildableName="SchoolHelperIOSTests.xctest" BlueprintName="SchoolHelperIOSTests" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/></TestableReference></Testables></TestAction>
  <LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" debugServiceExtension="internal" allowLocationSimulation="YES"><BuildableProductRunnable runnableDebuggingMode="0"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ids["app_target"]}" BuildableName="SchoolHelperIOS.app" BlueprintName="SchoolHelperIOS" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/></BuildableProductRunnable></LaunchAction>
  <ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES"><BuildableProductRunnable runnableDebuggingMode="0"><BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{ids["app_target"]}" BuildableName="SchoolHelperIOS.app" BlueprintName="SchoolHelperIOS" ReferencedContainer="container:SchoolHelperIOS.xcodeproj"/></BuildableProductRunnable></ProfileAction>
  <AnalyzeAction buildConfiguration="Debug"/>
  <ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES"/>
</Scheme>
'''
(scheme_dir / 'SchoolHelperIOS.xcscheme').write_text(scheme)
print('regenerated', proj_dir)
