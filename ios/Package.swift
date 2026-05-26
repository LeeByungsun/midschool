// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SchoolHelperIOSCore",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "SchoolHelperIOSCore",
            targets: ["SchoolHelperIOSCore"]
        )
    ],
    targets: [
        .target(
            name: "SchoolHelperIOSCore",
            path: "SchoolHelperIOS",
            exclude: [
                "App/RootTabView.swift",
                "App/SchoolHelperIOSApp.swift",
                "Info.plist",
                "SchoolHelperIOS.entitlements",
                "Features/Home/HomeView.swift",
                "Features/Meals/MealsView.swift",
                "Features/Schedule/ScheduleView.swift",
                "Features/Settings/SettingsView.swift",
                "Features/Setup/SetupView.swift",
                "Features/Timer/TimerView.swift",
                "Features/Timetable/TimetableView.swift",
                "Resources"
            ],
            sources: [
                "App/AppState.swift",
                "App/AppLaunchOverrides.swift",
                "Core/Models",
                "Core/Notifications",
                "Core/Networking",
                "Core/Repositories",
                "Core/Storage",
                "Features/Home/HomeViewModel.swift",
                "Features/Meals/MealsViewModel.swift",
                "Features/Schedule/ScheduleViewModel.swift",
                "Features/Settings/SettingsViewModel.swift",
                "Features/Setup/SetupViewModel.swift",
                "Features/Timer/TimerViewModel.swift",
                "Features/Timetable/TimetableViewModel.swift"
            ]
        ),
        .testTarget(
            name: "SchoolHelperIOSCoreTests",
            dependencies: ["SchoolHelperIOSCore"],
            path: "Tests/SchoolHelperIOSCoreTests"
        )
    ]
)
