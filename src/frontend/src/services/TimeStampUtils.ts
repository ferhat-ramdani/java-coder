class TimestampUtils {
    static toHumanReadable(timestamp: number): string {
        const localDate = new Date(timestamp);
        return localDate.toLocaleString();
    }
}

export { TimestampUtils };