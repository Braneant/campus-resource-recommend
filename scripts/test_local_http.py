import unittest
from urllib.error import HTTPError
from urllib.request import Request

from local_http import NoRedirect, validate_local_base_url


class LocalHttpSafetyTest(unittest.TestCase):
    def test_accepts_explicit_ipv4_loopback_port(self):
        self.assertEqual('http://127.0.0.1:8081', validate_local_base_url('http://127.0.0.1:8081/'))

    def test_rejects_external_userinfo_and_ambiguous_urls(self):
        for url in ('http://localhost:8080@evil.example:80', 'http://127.0.0.1:8080@evil.example:80',
                    'http://evil.example:8080', 'https://127.0.0.1:8081', 'http://localhost:8081',
                    'http://127.0.0.1:8081/unexpected', 'http://127.0.0.1:8081?x=1',
                    'http://127.0.0.1', 'http://127.0.0.1:99999'):
            with self.subTest(url=url), self.assertRaises(ValueError):
                validate_local_base_url(url)

    def test_redirect_to_external_target_is_blocked(self):
        handler = NoRedirect()
        request = Request('http://127.0.0.1:8081/api/test')
        with self.assertRaises(HTTPError):
            handler.redirect_request(request, None, 302, 'Found', {}, 'http://example.com/')


if __name__ == '__main__':
    unittest.main()
