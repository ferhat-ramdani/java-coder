import {createContext, Setter, Accessor, useContext, Resource} from 'solid-js';
import { createSignal, createResource } from 'solid-js';
import {createStore, SetStoreFunction} from "solid-js/store"
import { LLM } from './interfaces/LLM';
import {Chat} from "./interfaces/Chat";
import chatService from "./services/ChatService";
import {Prompt} from "./interfaces/Prompt";

type myStorage = {
    curChatId: { accessor: Accessor<number | null>, setter: Setter<number | null> };
    selectedLLM: { accessor: Accessor<LLM | null>, setter: Setter<LLM | null> };
    curChatPrompts: { accessor: Accessor<Prompt[]>, setter: Setter<Prompt[]> };
    chats: { resource: Resource<Chat[]>, mutator: Setter<Chat[] | undefined>, refetcher:  () => any};
};

const fetchChats = async (): Promise<Chat[]> => {
    try {
        return await chatService.getChats();
    } catch (error) {
        console.error("Error fetching chats:", error);
        return [];
    }
};

function createStorage(): myStorage {
    const [curChatId, setCurChatId] = createSignal<number | null>(null);
    const [selectedLLM, setSelectedLLM] = createSignal<LLM | null>(null);
    const [curChatPrompts, setCurChatPrompts] = createSignal<Prompt[]>([]);
    const [chats, { mutate: setChats, refetch: refetchChats }] = createResource<Chat[]>(fetchChats);

    return {
        curChatId: { accessor: curChatId, setter: setCurChatId },
        selectedLLM: { accessor: selectedLLM, setter: setSelectedLLM },
        curChatPrompts: {accessor: curChatPrompts, setter: setCurChatPrompts},
        chats: { resource: chats, mutator: setChats, refetcher: refetchChats }
    };
}

const AppContext = createContext<[myStorage, SetStoreFunction<myStorage>]>();

export function ContextProvider(props: { children: any }) {
    const storageObject = createStorage();
    const [appStorage, setAppStorage] = createStore(storageObject);

    return (
        <AppContext.Provider value={[appStorage, setAppStorage]}>
            {props.children}
        </AppContext.Provider>
    );
}

export function useAppContext () : [myStorage, SetStoreFunction<myStorage>] {
    return useContext(AppContext)!;
}
