import { createClient } from '@supabase/supabase-js';
import {demoRead,demoVisible,demoCreate,demoProfiles,demoUpdate,demoSave,demoEvent} from './demo';
import type {Profile,Detail,Report,NewReport,Notice} from './types';
const env=import.meta.env;
export const demo=env.VITE_DEMO_MODE==='true';
const runtime: {supabaseUrl?:string;supabasePublishableKey?:string}=!demo&&!env.VITE_SUPABASE_PUBLISHABLE_KEY?await fetch('/api/config').then(r=>r.ok?r.json():{}).catch(()=>({})):{};
const supabaseUrl=env.VITE_SUPABASE_URL||runtime.supabaseUrl;
const publishableKey=env.VITE_SUPABASE_PUBLISHABLE_KEY||runtime.supabasePublishableKey;
export const supabase=supabaseUrl&&publishableKey?createClient(supabaseUrl,publishableKey):null;
export async function request<T>(path:string,body?:unknown,method?:string):Promise<T>{
 const session=await supabase?.auth.getSession();const token=session?.data.session?.access_token;
 const response=await fetch(`${env.VITE_API_URL||''}/api${path}`,{method:method||(body?'POST':'GET'),headers:{...(body instanceof FormData?{}:{'Content-Type':'application/json'}),...(token?{Authorization:`Bearer ${token}`}:{})},body:body?body instanceof FormData?body:JSON.stringify(body):undefined});
 if(!response.ok){const error=await response.json().catch(()=>({}));throw Error(error.message||`Request failed (${response.status}). Please try again.`)}
 const text=await response.text();return text?JSON.parse(text):undefined as T;
}
export const api={
 async list(p:Profile,q='',status='',page=0):Promise<{content:Report[];totalElements:number;totalPages:number}>{if(!demo)return request(`/complaints?q=${encodeURIComponent(q)}&status=${status}&page=${page}`);const data=demoRead().filter(d=>demoVisible(p,d)).map(d=>d.complaint).filter(c=>(!q||`${c.title} ${c.trackingNumber}`.toLowerCase().includes(q.toLowerCase()))&&(!status||c.status===status));return {content:data.slice(page*30,(page+1)*30),totalElements:data.length,totalPages:Math.ceil(data.length/30)}},
 async detail(p:Profile,id:string):Promise<Detail>{if(!demo)return request(`/complaints/${id}`);const d=demoRead().find(d=>d.complaint.id===id&&demoVisible(p,d));if(!d)throw Error('Report not found');return {...d,events:d.events.filter(e=>!e.internal||p.role!=='CITIZEN')}},
 async create(p:Profile,r:NewReport):Promise<Report>{return demo?demoCreate(r,p):request('/complaints',r)},
 async update(p:Profile,id:string,action:string,body:Record<string,unknown>){return demo?demoUpdate(id,p,action,body):request(`/complaints/${id}/${action}`,body)},
 async staff():Promise<Profile[]>{return demo?[demoProfiles.OFFICER]:request('/staff')},
 async notices():Promise<Notice[]>{return demo?[]:request('/notifications')},
 async upload(p:Profile,id:string,file:File,kind:string){if(!['image/jpeg','image/png'].includes(file.type)||file.size>3*1024*1024)throw Error('Choose a JPEG or PNG image under 3 MB.');if(!demo){const data=new FormData();data.append('file',file);data.append('kind',kind);return request(`/complaints/${id}/attachments`,data)}
 const data=demoRead(),d=data.find(d=>d.complaint.id===id);if(!d)throw Error('Report not found');const url=await new Promise<string>((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result));reader.onerror=reject;reader.readAsDataURL(file)});d.attachments.push({id:crypto.randomUUID(),kind,objectPath:url,contentType:file.type});demoEvent(d,p,'EVIDENCE','Photographic evidence added.');demoSave(data);
 },
 async image(id:string,path:string){if(demo)return path;const result=await request<{signedURL:string}>(`/attachments/${id}/url`);return supabaseUrl+'/storage/v1'+result.signedURL}
};
