import { Container, getContainer } from "@cloudflare/containers";
import { env } from "cloudflare:workers";

export class BankingApiContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";

  envVars = {
    JWT_SECRET: env.JWT_SECRET,
    JWT_EXPIRATION_MS: env.JWT_EXPIRATION_MS ?? "3600000",
    CORS_ALLOWED_ORIGINS: env.CORS_ALLOWED_ORIGINS,
  };
}

export default {
  async fetch(request, env) {
    return getContainer(env.BANKING_API_CONTAINER).fetch(request);
  },
};
