import SwiftUI

struct HomeView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("요약") {
                    Text("오늘 시간표")
                    Text("오늘 급식")
                    Text("다가오는 일정")
                    Text("타이머")
                }
            }
            .navigationTitle("학교도우미")
        }
    }
}
