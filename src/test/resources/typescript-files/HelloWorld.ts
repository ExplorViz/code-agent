// Simple TypeScript file for testing

export interface Person {
    name: string;
    age: number;
}

export class Greeter {
    private greeting: string;

    constructor(message: string) {
        this.greeting = message;
    }

    public greet(person: Person): string {
        return `${this.greeting}, ${person.name}!`;
    }
}

export function sayHello(name: string): void {
    console.log(`Hello, ${name}!`);
}

