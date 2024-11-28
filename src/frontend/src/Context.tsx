import {createContext, Setter, Accessor, useContext, Resource, createEffect} from 'solid-js';
import { createSignal, createResource } from 'solid-js';
import {createStore, SetStoreFunction} from "solid-js/store"
import {Chat} from "./interfaces/Chat";
import chatService from "./services/ChatService";
import llmService from "./services/LLMService";
import {Utils} from "./services/Utils";

type myStorage = {
    chats: { resource: Resource<Chat[]>, mutator: Setter<Chat[] | undefined>, refetcher:  () => any};
    pageTitle: {accessor: Accessor<string>, setter: Setter<string>};
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
    const [chats, { mutate: setChats, refetch: refetchChats }] = createResource<Chat[]>(fetchChats);
    const [pageTitle, setPageTitle] = createSignal("");

    return {
        chats: { resource: chats, mutator: setChats, refetcher: refetchChats},
        pageTitle: { accessor: pageTitle, setter: setPageTitle }
    };
}

const AppContext = createContext<[myStorage, SetStoreFunction<myStorage>]>();

export function ContextProvider(props: { children: any }) {
    const storageObject = createStorage();
    const [appStorage, setAppStorage] = createStore(storageObject);

    createEffect(() => {
    const storeDefaultLLM = async () => {
        try {
            const storedLLM = localStorage.getItem('default-llm');
            if (!storedLLM) {
                const llms = await llmService.getLLMS();
                if (llms.length > 0) {
                    localStorage.setItem('default-llm', JSON.stringify(llms[0].id));
                }
            }
        } catch (error) {
            Utils.showToast("Error", "Error while saving default llm local storage", "danger", "bi-exclamation-triangle");
        }
    };
    storeDefaultLLM();
}, []);

    return (
        <AppContext.Provider value={[appStorage, setAppStorage]}>
            {props.children}
        </AppContext.Provider>
    );
}

export function useAppContext () : [myStorage, SetStoreFunction<myStorage>] {
    return useContext(AppContext)!;
}
