import { createServer } from '../frontend/node_modules/vite/dist/node/index.js'
import config from '../frontend/vite.config.js'
import { fileURLToPath } from 'node:url'
const root = fileURLToPath(new URL('../frontend/', import.meta.url))
const [port, backend, identity] = process.argv.slice(2)
const server = await createServer({
  ...config,
  configFile: false,
  root,
  cacheDir: fileURLToPath(new URL('./vite-cache/', import.meta.url)),
  plugins: [...config.plugins, {
    name: 'campus-launcher-identity',
    configureServer(server) {
      server.middlewares.use('/__campus_launcher', (_req, res) => {
        res.setHeader('Content-Type', 'application/json')
        res.end(JSON.stringify({ identity, backend: Number(backend) }))
      })
    }
  }],
  server: {
    ...config.server,
    host: '127.0.0.1', port: Number(port), strictPort: true,
    proxy: { ...config.server.proxy, '/api': {
      ...config.server.proxy['/api'], target: `http://127.0.0.1:${backend}`
    }}
  }
})
await server.listen()
server.printUrls()
