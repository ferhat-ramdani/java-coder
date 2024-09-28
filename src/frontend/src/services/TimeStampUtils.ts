class TimestampUtils {
    static toLocalUnix(timestamp: number): number {
        const localDate = new Date(timestamp * 1000);
        return Math.floor(localDate.getTime() / 1000);
    }

    static toHumanReadable(timestamp: number): string {
        const localDate = new Date(timestamp * 1000);
        return localDate.toLocaleString();
    }
}

export { TimestampUtils };