export const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
export const DEFAULTS = {start:'2026-10-19',end:'2027-01-31',days:[1,3,5],time:'18:00',calories:2300,protein:145,carbs:270,fat:71,theme:'dark',name:''};
const ex=(id,name,sets,reps,group,cue)=>({id,name,sets,reps,group,cue});
export const LEGACY_PLAN = [
 {name:'Chest & biceps A',subtitle:'Press, curl, build a base.',exercises:[
 ex('press','Machine chest press',3,'8–12','Chest','Keep shoulder blades against the pad. Use a comfortable range.'),
 ex('cablecurl','Cable biceps curl',3,'10–15','Biceps','Keep elbows still. Lower with control.'),
 ex('row','Seated machine row',2,'8–12','Back','Pull elbows back without rocking your torso.'),
 ex('legpress','Leg press',2,'10–15','Legs + glutes','Keep hips on the pad. Use a pain-free depth.'),
 ex('fly','Pec deck',1,'10–15','Chest','Keep a soft bend in your elbows; avoid forcing a stretch.'),
 ex('legcurl','Seated leg curl',2,'10–15','Hamstrings','Keep hips down. Control the return.'),
 ex('pushdown','Triceps pushdown',2,'10–15','Triceps','Keep upper arms still; straighten elbows comfortably.') ]},
 {name:'Chest & biceps B',subtitle:'Incline focus. A little glute work.',exercises:[
 ex('incline','Incline dumbbell press',3,'8–12','Chest','Use a low incline. Log the weight of ONE dumbbell.'),
 ex('preacher','Machine preacher curl',3,'10–15','Biceps','Keep upper arms on the pad. Avoid snapping elbows straight.'),
 ex('pulldown','Lat pulldown',2,'8–12','Back','Pull toward upper chest; avoid swinging.'),
 ex('hipthrust','Machine hip thrust',2,'10–15','Glutes','Keep ribs down. Finish by squeezing glutes, not arching your back.'),
 ex('legext','Leg extension',2,'10–15','Quads','Align knees with the machine pivot and move smoothly.'),
 ex('lateral','Dumbbell lateral raise',2,'12–20','Shoulders','Log ONE dumbbell. Lift smoothly to a comfortable height.') ]},
 {name:'Chest & biceps C',subtitle:'Chest volume. Finish with arms.',exercises:[
 ex('press','Machine chest press',3,'8–12','Chest','Use the same machine and setup as session A.'),
 ex('cablecurl','Cable biceps curl',3,'10–15','Biceps','Keep the same attachment and setup as session A.'),
 ex('fly','Pec deck',2,'10–15','Chest','Move with control and a comfortable stretch.'),
 ex('row','Seated machine row',2,'8–12','Back','Keep chest steady and avoid shrugging.'),
 ex('legpress','Leg press',2,'10–15','Legs + glutes','Use the same setup as session A.'),
 ex('hammer','Dumbbell hammer curl',1,'10–15','Biceps','Palms face in. Log ONE dumbbell.'),
 ex('legcurl','Seated leg curl',2,'10–15','Hamstrings','Lower slowly without letting the stack slam.'),
 ex('lateral','Dumbbell lateral raise',2,'12–20','Shoulders','Log ONE dumbbell. Avoid swinging.') ]}
];

export const PLAN = [
 {name:'Chest & arms A',subtitle:'Flat press · arms · forearms',exercises:[
 ex('press','Machine chest press',3,'8–12','Chest','Keep shoulder blades against the pad. Rest about 2 minutes between sets.'),
 ex('row','Seated machine row',2,'8–12','Back','Pull elbows back without rocking your torso.'),
 ex('legpress','Leg press',2,'10–15','Legs + glutes','Keep hips on the pad. Use a comfortable depth.'),
 ex('cablecurl','Cable biceps curl',2,'10–15','Biceps','Pair with pushdowns: curl set, pushdown set, then rest 60–90 seconds.'),
 ex('pushdown','Triceps pushdown',2,'10–15','Triceps','Keep upper arms still. Pair with cable curls if both stations are free.'),
 ex('wristcurl','Seated dumbbell wrist curl',1,'12–20','Forearms','Forearms supported, palms up. Use light weight and a comfortable range. Log one dumbbell.') ]},
 {name:'Chest & arms B',subtitle:'Incline press · arms · glutes',exercises:[
 ex('incline','Incline dumbbell press',3,'8–12','Chest','Low incline. Log ONE dumbbell. Rest about 2 minutes between sets.'),
 ex('pulldown','Lat pulldown',2,'8–12','Back','Pull toward upper chest without swinging.'),
 ex('hipthrust','Machine hip thrust',2,'10–15','Glutes','Keep ribs down; avoid arching your lower back.'),
 ex('preacher','Machine preacher curl',2,'10–15','Biceps','Pair with overhead triceps extensions: one set each, then rest 60–90 seconds.'),
 ex('overhead','Cable overhead triceps extension',2,'10–15','Triceps','Keep upper arms steady. Use a comfortable elbow and shoulder position.'),
 ex('wristext','Seated dumbbell wrist extension',1,'12–20','Forearms','Forearms supported, palms down. Use a very light weight, no forced stretch. Log one dumbbell.'),
 ex('lateral','Dumbbell lateral raise',1,'12–20','Shoulders','Log ONE dumbbell. Lift smoothly without swinging.') ]},
 {name:'Chest & arms C',subtitle:'Chest variety · hammer curls · triceps',exercises:[
 ex('press','Machine chest press',2,'8–12','Chest','Use the same machine and setup as A. Rest about 2 minutes.'),
 ex('fly','Pec deck',2,'10–15','Chest','Keep a soft elbow bend. Use a comfortable stretch.'),
 ex('row','Seated machine row',2,'8–12','Back','Keep chest steady and avoid shrugging.'),
 ex('legcurl','Seated leg curl',2,'10–15','Hamstrings','Keep hips down; control the return.'),
 ex('hammer','Dumbbell hammer curl',2,'10–15','Biceps + forearms','Palms face in. Log ONE dumbbell. Pair with pushdowns, then rest 60–90 seconds.'),
 ex('pushdown','Triceps pushdown',2,'10–15','Triceps','Keep upper arms still; straighten elbows comfortably.'),
 ex('lateral','Dumbbell lateral raise',1,'12–20','Shoulders','Log ONE dumbbell. Avoid swinging.') ]}
];
export const EXERCISES = [...new Map([...PLAN,...LEGACY_PLAN].flatMap(p=>p.exercises).map(e=>[e.id,e])).values()];
export function validatePlan(plans){
 if(!Array.isArray(plans)||plans.length!==3)throw Error('A plan needs three sessions.');
 return plans.map((p,i)=>{
  if(!p||!Array.isArray(p.exercises)||p.exercises.length<1||p.exercises.length>10)throw Error('Each session needs 1–10 exercises.');
  const ids=new Set();
  const exercises=p.exercises.map(e=>{
   if(!e||typeof e.id!=='string'||!EXERCISES.some(x=>x.id===e.id)||ids.has(e.id)||!Number.isInteger(e.sets)||e.sets<1||e.sets>5||!/^\d{1,2}[–-]\d{1,2}$/.test(e.reps))throw Error('Invalid or duplicate exercise.');
   const [lo,hi]=e.reps.split(/[–-]/).map(Number);if(lo<1||hi<lo||hi>50)throw Error('Rep ranges must be between 1 and 50.');
   ids.add(e.id);const base=EXERCISES.find(x=>x.id===e.id);return {...base,cue:typeof e.cue==='string'?e.cue.slice(0,1000):base.cue,sets:e.sets,reps:lo+'–'+hi};
  });
  return {name:PLAN[i].name,subtitle:PLAN[i].subtitle,exercises};
 });
}
export const estimatedMinutes=exercises=>Math.round(9+exercises.length*.7+exercises.reduce((n,e)=>n+e.sets*(e.group==='Chest'||e.group==='Back'||e.group.includes('Legs')||e.group==='Glutes'?2.5:1.7),0));

export const normalizeName=value=>typeof value==='string'?Array.from(value.replace(/[\\u0000-\\u001f\\u007f]/g,'').trim().replace(/\\s+/g,' ')).slice(0,40).join(''):'';
export const iso=(d=new Date())=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
export const dateObj=s=>new Date(`${s}T12:00:00`);
export const addDays=(s,n)=>{const d=dateObj(s);d.setDate(d.getDate()+n);return iso(d)};
export const weekStart=s=>addDays(s,-((dateObj(s).getDay()+6)%7));
export const programWeek=(s,start)=>Math.max(1,Math.floor((Date.UTC(...s.split('-').map((v,i)=>Number(v)-(i===1?1:0)))-Date.UTC(...start.split('-').map((v,i)=>Number(v)-(i===1?1:0))))/604800000)+1);
export const setCount=(e,day,settings)=>programWeek(day,settings.start)<=2?Math.min(e.sets,2):e.sets;
export const nextSession=(workouts,day)=>Object.values(workouts).filter(w=>w.date<day&&w.completed).length%3;
export const lastExercise=(workouts,id,day)=>Object.values(workouts).filter(w=>w.date<day&&w.completed&&w.exercises?.[id]?.some(s=>s.done)).sort((a,b)=>b.date.localeCompare(a.date))[0]?.exercises[id]?.filter(s=>s.done) || [];
export function chartPoints(entries,width=300,height=100){
 if(!entries.length)return [];
 const values=entries.map(e=>e[1]),lo=Math.min(...values),hi=Math.max(...values),span=hi-lo||1;
 const dates=entries.map(e=>Date.parse(`${e[0]}T12:00:00Z`)),timeSpan=dates.at(-1)-dates[0];
 return entries.map((e,i)=>({x:timeSpan?10+(dates[i]-dates[0])/timeSpan*(width-20):width/2,y:hi===lo?height/2:10+(hi-e[1])/span*(height-20),date:e[0],value:e[1]}));
}
export function calendarEvent(settings,weekday,origin=''){
 let day=iso(); if(day<settings.start)day=settings.start;
 for(let i=0;i<7&&dateObj(day).getDay()!==weekday;i++)day=addDays(day,1);
 if(day>settings.end)return null;
 const start=day.replaceAll('-','')+'T'+settings.time.replace(':','')+'00';
 const endDate=new Date(`${day}T${settings.time}:00`);endDate.setMinutes(endDate.getMinutes()+45);
 const end=iso(endDate).replaceAll('-','')+'T'+String(endDate.getHours()).padStart(2,'0')+String(endDate.getMinutes()).padStart(2,'0')+'00';
 const params=new URLSearchParams({action:'TEMPLATE',text:'Steady · Gym',dates:`${start}/${end}`,details:`Your next 45-minute chest & arms session. Open Steady and follow A → B → C.\n${origin}\nSet your preferred notification before saving.`,recur:`RRULE:FREQ=WEEKLY;BYDAY=${['SU','MO','TU','WE','TH','FR','SA'][weekday]};UNTIL=${settings.end.replaceAll('-','')}T235959`});
 return {day,start,end,url:'https://calendar.google.com/calendar/render?'+params};
}
const validDate=s=>typeof s==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(s)&&!Number.isNaN(dateObj(s).getTime())&&iso(dateObj(s))===s;
const number=(n,min,max)=>typeof n==='number'&&Number.isFinite(n)&&n>=min&&n<=max;
export function validateBackup(raw){
 if(raw?.version!==1||!raw.settings||!raw.logs||!raw.workouts||!Array.isArray(raw.photos))throw Error('This is not a Steady v1 backup.');
 const s=raw.settings;
 if(!validDate(s.start)||!validDate(s.end)||s.start>s.end||!Array.isArray(s.days)||s.days.length!==3||new Set(s.days).size!==3||s.days.some(d=>!Number.isInteger(d)||d<0||d>6)||!/^([01]\d|2[0-3]):[0-5]\d$/.test(s.time)||!['light','dark'].includes(s.theme))throw Error('Invalid schedule in backup.');
 for(const k of ['calories','protein','carbs','fat'])if(!number(s[k],0,10000))throw Error('Invalid targets in backup.');
 const clean={version:1,settings:{...DEFAULTS,...Object.fromEntries(Object.keys(DEFAULTS).map(k=>[k,s[k]]))},logs:{},workouts:{},photos:[]};
 for(const [date,log] of Object.entries(raw.logs)){
  if(!validDate(date)||!log||typeof log!=='object')throw Error('Invalid daily entry.');
  const out={};for(const k of ['calories','protein','carbs','fat','weight','waist','steps'])if(log[k]!==undefined&&log[k]!==null){if(!number(log[k],0,100000))throw Error('Invalid daily value.');out[k]=log[k]}
  if(log.steps!==undefined&&(!Number.isInteger(log.steps)||log.steps<0))throw Error('Invalid steps.');if(typeof log.stepsSource==='string')out.stepsSource=log.stepsSource.slice(0,100);if(typeof log.stepsSyncedAt==='string')out.stepsSyncedAt=log.stepsSyncedAt.slice(0,40);if(log.nutritionSaved===true){if(['calories','protein','carbs','fat'].some(k=>out[k]===undefined))throw Error('Missing nutrition totals.');out.nutritionSaved=true;}clean.logs[date]=out;
 }
 for(const [date,w] of Object.entries(raw.workouts)){
  if(!validDate(date)||w.date!==date||!Number.isInteger(w.session)||w.session<0||w.session>2||typeof w.exercises!=='object')throw Error('Invalid workout.');
  const original=w.plan?validatePlan([0,1,2].map(i=>({exercises:i===w.session?w.plan:PLAN[i].exercises})))[w.session].exercises:LEGACY_PLAN[w.session].exercises;
  const out={date,session:w.session,completed:w.completed===true,exercises:{}};
  if(w.plan){out.plan=original;out.name=typeof w.name==='string'?w.name.slice(0,100):PLAN[w.session].name;}if(w.neckDone===true)out.neckDone=true;
  for(const e of original){const rows=w.exercises[e.id];if(!Array.isArray(rows)||rows.length>10)throw Error('Invalid workout sets.');out.exercises[e.id]=rows.map(r=>{if(!((r.kg===''||number(r.kg,0,2000))&&(r.reps===''||Number.isInteger(r.reps)&&number(r.reps,1,100))&&typeof r.done==='boolean') )throw Error('Invalid set.');if(r.done&&(r.kg===''||r.reps===''))throw Error('Completed set has no values.');return {kg:r.kg,reps:r.reps,done:r.done}})}
  clean.workouts[date]=out;
 }
 for(const p of raw.photos){if(typeof p.id!=='string'||p.id.length>100||!validDate(p.date)||!/^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/.test(p.src)||p.src.length>12000000)throw Error('Invalid photo.');clean.photos.push({id:p.id,date:p.date,src:p.src,view:['Front','Side','Back'].includes(p.view)?p.view:'Front'})}
 clean.settings.name=normalizeName(s.name);if(raw.plans)clean.plans=validatePlan(raw.plans);
 return clean;
}





