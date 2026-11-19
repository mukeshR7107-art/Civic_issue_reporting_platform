export type Role = 'CITIZEN'|'SUPERVISOR'|'OFFICER'|'ADMIN';
export type Status = 'SUBMITTED'|'ACKNOWLEDGED'|'ASSIGNED'|'IN_PROGRESS'|'RESOLUTION_PROPOSED'|'CLOSED'|'REOPENED'|'REJECTED'|'NEEDS_INFORMATION';
export type Priority = 'LOW'|'NORMAL'|'HIGH'|'CRITICAL';
export interface Profile {id:string;name:string;role:Role;department:string|null;area:string|null}
export interface Report {id:string;version:number;trackingNumber:string;reporterId:string;title:string;description:string;category:string;area:string;landmark:string;latitude:number;longitude:number;department:string|null;officerId:string|null;status:Status;priority:Priority;urgency:string;createdAt:string;updatedAt:string;dueAt:string;proposedAt:string|null;closedAt:string|null}
export interface Event {id:string;actorName:string;action:string;message:string;internal:boolean;createdAt:string}
export interface Attachment {id:string;kind:string;objectPath:string;contentType:string}
export interface Detail {complaint:Report;events:Event[];attachments:Attachment[]}
export interface Notice {id:string;complaintId:string;message:string;seen:boolean;createdAt:string}
export interface NewReport {title:string;description:string;category:string;area:string;landmark:string;latitude:number;longitude:number;urgency:string;requestKey:string}
export const categories=['Streetlight','Garbage','Pothole','Water leakage','Public facility'];
export const areas=['Ward A','Ward B','Ward C'];
export const statuses:Status[]=['SUBMITTED','NEEDS_INFORMATION','ACKNOWLEDGED','ASSIGNED','IN_PROGRESS','RESOLUTION_PROPOSED','CLOSED','REOPENED','REJECTED'];
export const label=(value:string)=>value.toLowerCase().replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
export const departments:Record<string,string>={'Streetlight':'Electrical maintenance','Garbage':'Sanitation','Pothole':'Roads and transport','Water leakage':'Water supply','Public facility':'Public works'};
export function actions(p:Profile,c:Report):Status[] {
 const own=p.id===c.reporterId, manager=p.role==='ADMIN'||p.role==='SUPERVISOR'&&p.department===c.department&&p.area===c.area;
 const officer=p.role==='OFFICER'&&p.id===c.officerId;
 if(own&&c.status==='NEEDS_INFORMATION')return ['SUBMITTED'];
 if(own&&c.status==='RESOLUTION_PROPOSED')return ['CLOSED','REOPENED'];
 if(own&&c.status==='CLOSED'&&c.proposedAt&&Date.now()-Date.parse(c.proposedAt)<7*86400000)return ['REOPENED'];
 if(manager&&c.status==='SUBMITTED')return ['ACKNOWLEDGED','NEEDS_INFORMATION','REJECTED'];
 if(officer&&c.status==='ASSIGNED')return ['IN_PROGRESS'];
 if(officer&&c.status==='IN_PROGRESS')return ['RESOLUTION_PROPOSED'];
 return [];
}
