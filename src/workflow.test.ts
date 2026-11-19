import {describe,it,expect} from 'vitest';
import {actions,type Report} from './types';
import {demoProfiles} from './demo';
const report={reporterId:'citizen',officerId:'officer',department:'Electrical maintenance',area:'Ward A',status:'SUBMITTED'} as Report;
describe('workflow controls',()=>{
 it('does not expose staff actions to citizens',()=>expect(actions(demoProfiles.CITIZEN,report)).toEqual([]));
 it('scopes supervisors to their department and area',()=>{expect(actions(demoProfiles.SUPERVISOR,report)).toContain('ACKNOWLEDGED');expect(actions(demoProfiles.SUPERVISOR,{...report,area:'Ward B'})).toEqual([])});
 it('lets only the assigned officer start work',()=>{expect(actions(demoProfiles.OFFICER,{...report,status:'ASSIGNED'})).toEqual(['IN_PROGRESS']);expect(actions({...demoProfiles.OFFICER,id:'other'},{...report,status:'ASSIGNED'})).toEqual([])});
 it('separates a proposed fix from closure',()=>expect(actions(demoProfiles.CITIZEN,{...report,status:'RESOLUTION_PROPOSED'})).toEqual(['CLOSED','REOPENED']));
 it('does not offer reopening after the deadline',()=>expect(actions(demoProfiles.CITIZEN,{...report,status:'CLOSED',proposedAt:'2020-01-01T00:00:00Z'})).toEqual([]));
});
