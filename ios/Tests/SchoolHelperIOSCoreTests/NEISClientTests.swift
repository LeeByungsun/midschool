import XCTest
@testable import SchoolHelperIOSCore

final class NEISClientTests: XCTestCase {
    override func tearDown() {
        StubURLProtocol.requestHandler = nil
        super.tearDown()
    }

    func testSearchSchoolsWorksWithoutApiKeyAndOmitsKeyQueryItem() async throws {
        let session = URLSession(configuration: stubbedConfiguration())
        let client = NEISClient(
            session: session,
            config: NEISClient.Config(
                baseURL: URL(string: "https://example.com/")!,
                apiKey: "",
                noticesBaseURL: URL(string: "https://example.com/")!
            )
        )
        StubURLProtocol.requestHandler = { request in
            let components = URLComponents(url: request.url!, resolvingAgainstBaseURL: false)
            XCTAssertNil(components?.queryItems?.first { $0.name == "KEY" })
            XCTAssertEqual(components?.queryItems?.first { $0.name == "SCHUL_NM" }?.value, "미사중학교")
            return (HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!, Self.schoolInfoPayload)
        }

        let schools = try await client.searchSchools(query: "미사중학교")

        XCTAssertEqual(schools.map(\.schoolName), ["미사중학교"])
        XCTAssertEqual(schools.first?.schoolCode, "7692129")
    }

    func testFetchNoticesBuildsBFFQueryAndDecodesPreviewItems() async throws {
        let session = URLSession(configuration: stubbedConfiguration())
        let client = NoticesClient(
            session: session,
            config: NEISClient.Config(
                baseURL: URL(string: "https://example.com/")!,
                apiKey: "",
                noticesBaseURL: URL(string: "https://web.example.com/")!
            )
        )
        StubURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.host, "web.example.com")
            XCTAssertEqual(request.url?.path, "/api/notices")
            let components = URLComponents(url: request.url!, resolvingAgainstBaseURL: false)
            XCTAssertEqual(components?.queryItems?.first { $0.name == "officeCode" }?.value, "J10")
            XCTAssertEqual(components?.queryItems?.first { $0.name == "schoolCode" }?.value, "7531093")
            XCTAssertEqual(components?.queryItems?.first { $0.name == "limit" }?.value, "10")
            return (
                HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!,
                Self.noticeSuccessPayload
            )
        }

        let notices = try await client.fetchNotices(
            officeCode: "J10",
            schoolCode: "7531093",
            limit: 99
        )

        XCTAssertEqual(notices, [
            NoticePreview(
                id: "notice-1",
                title: "학부모 공지",
                date: "",
                author: "행정실",
                url: "https://web.example.com/notices/1"
            )
        ])
    }

    func testFetchNoticesThrowsServerMessageForFailurePayload() async {
        let session = URLSession(configuration: stubbedConfiguration())
        let client = NoticesClient(
            session: session,
            config: NEISClient.Config(
                baseURL: URL(string: "https://example.com/")!,
                apiKey: "",
                noticesBaseURL: URL(string: "https://web.example.com/")!
            )
        )
        StubURLProtocol.requestHandler = { request in
            (
                HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!,
                Self.noticeFailurePayload
            )
        }

        do {
            _ = try await client.fetchNotices(
                officeCode: "J10",
                schoolCode: "7531093",
                limit: 3
            )
            XCTFail("Expected server error")
        } catch {
            XCTAssertEqual(error.localizedDescription, "학교 홈페이지 주소를 찾지 못했어요.")
        }
    }

    private func stubbedConfiguration() -> URLSessionConfiguration {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return configuration
    }

    private static let schoolInfoPayload = Data(
        """
        {
          "schoolInfo": [
            {"head": [{"list_total_count": 1}, {"RESULT": {"CODE": "INFO-000", "MESSAGE": "정상 처리되었습니다."}}]},
            {"row": [{
              "ATPT_OFCDC_SC_CODE": "J10",
              "ATPT_OFCDC_SC_NM": "경기도교육청",
              "SD_SCHUL_CODE": "7692129",
              "SCHUL_NM": "미사중학교",
              "SCHUL_KND_SC_NM": "중학교",
              "ORG_RDNMA": "경기도 하남시 미사강변한강로334번길 70"
            }]}
          ]
        }
        """.utf8
    )

    private static let noticeSuccessPayload = Data(
        """
        {
          "status": "success",
          "items": [{
            "id": "notice-1",
            "title": "학부모 공지",
            "date": "",
            "author": "행정실",
            "url": "https://web.example.com/notices/1"
          }]
        }
        """.utf8
    )

    private static let noticeFailurePayload = Data(
        """
        {
          "status": "error",
          "items": [],
          "message": "학교 홈페이지 주소를 찾지 못했어요."
        }
        """.utf8
    )
}

private final class StubURLProtocol: URLProtocol {
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let requestHandler = Self.requestHandler else {
            client?.urlProtocol(self, didFailWithError: URLError(.badServerResponse))
            return
        }

        do {
            let (response, data) = try requestHandler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}
