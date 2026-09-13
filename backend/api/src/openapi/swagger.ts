import { Router } from 'express';
import swaggerUi from 'swagger-ui-express';
import { openApiSpec } from './spec';

export const swaggerRouter = Router();

swaggerRouter.use('/', swaggerUi.serve);
swaggerRouter.get('/', swaggerUi.setup(openApiSpec, {
  customSiteTitle: 'Porter Logistics Platform API Docs',
  customCss: '.swagger-ui .topbar { display: none }',
  swaggerOptions: {
    persistAuthorization: true,
    displayRequestDuration: true
  }
}));

swaggerRouter.get('/json', (_req, res) => {
  res.json(openApiSpec);
});
