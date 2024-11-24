import defaultConfig from './configuration/config.json';

class Config {
  private static instance: Config;
  private readonly backendUrl: string;

  private constructor() {
    this.backendUrl = process.env.BACKEND_URL || defaultConfig.backendUrl || 'http://localhost:8080';
  }
  public static getInstance(): Config {
    if (!Config.instance) {
      Config.instance = new Config();
    }
    return Config.instance;
  }

  public getBackendUrl(): string {
    return this.backendUrl;
  }
}

export default Config;
