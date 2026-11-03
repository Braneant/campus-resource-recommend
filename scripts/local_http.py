"""HTTP helpers for disposable local-only verification scripts."""
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, ProxyHandler, build_opener


def validate_local_base_url(value):
    try:
        parts = urlsplit(value)
        port = parts.port
    except ValueError as error:
        raise ValueError('Expected an explicit IPv4 loopback HTTP URL and port') from error
    if (parts.scheme != 'http' or parts.hostname != '127.0.0.1' or port is None
            or parts.netloc != f'127.0.0.1:{port}' or not 1 <= port <= 65535
            or parts.path not in ('', '/') or parts.query or parts.fragment):
        raise ValueError('Expected an explicit IPv4 loopback HTTP URL and port')
    return f'http://127.0.0.1:{port}'


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise HTTPError(req.full_url, code, 'Redirects are forbidden for local verification', headers, fp)


def local_opener():
    return build_opener(ProxyHandler({}), NoRedirect())
