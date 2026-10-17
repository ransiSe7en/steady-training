export const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
export const DEFAULTS = {start:'2026-10-19',end:'2027-01-31',days:[1,3,5],time:'18:00',calories:2300,protein:145,carbs:270,fat:71,theme:'dark'};
const ex=(id,name,sets,reps,group,cue)=>({id,name,sets,reps,group,cue});
export const PLAN = [
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
 const endDate=new Date(`${day}T${settings.time}:00`);endDate.setHours(endDate.getHours()+1);
 const end=iso(endDate).replaceAll('-','')+'T'+String(endDate.getHours()).padStart(2,'0')+String(endDate.getMinutes()).padStart(2,'0')+'00';
 const params=new URLSearchParams({action:'TEMPLATE',text:'Steady · Gym',dates:`${start}/${end}`,details:`Your next chest & biceps focused session. Open Steady and follow A → B → C.\n${origin}\nSet your preferred notification before saving.`,recur:`RRULE:FREQ=WEEKLY;BYDAY=${['SU','MO','TU','WE','TH','FR','SA'][weekday]};UNTIL=${settings.end.replaceAll('-','')}T235959`});
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
  const out={};for(const k of ['calories','protein','carbs','fat','weight','waist'])if(log[k]!==undefined&&log[k]!==null){if(!number(log[k],0,100000))throw Error('Invalid daily value.');out[k]=log[k]}
  if(log.nutritionSaved===true){if(['calories','protein','carbs','fat'].some(k=>out[k]===undefined))throw Error('Missing nutrition totals.');out.nutritionSaved=true;}clean.logs[date]=out;
 }
 for(const [date,w] of Object.entries(raw.workouts)){
  if(!validDate(date)||w.date!==date||!Number.isInteger(w.session)||w.session<0||w.session>2||typeof w.exercises!=='object')throw Error('Invalid workout.');
  const out={date,session:w.session,completed:w.completed===true,exercises:{}};
  for(const e of PLAN[w.session].exercises){const rows=w.exercises[e.id];if(!Array.isArray(rows)||rows.length>10)throw Error('Invalid workout sets.');out.exercises[e.id]=rows.map(r=>{if(!((r.kg===''||number(r.kg,0,2000))&&(r.reps===''||Number.isInteger(r.reps)&&number(r.reps,1,100))&&typeof r.done==='boolean') )throw Error('Invalid set.');if(r.done&&(r.kg===''||r.reps===''))throw Error('Completed set has no values.');return {kg:r.kg,reps:r.reps,done:r.done}})}
  clean.workouts[date]=out;
 }
 for(const p of raw.photos){if(typeof p.id!=='string'||p.id.length>100||!validDate(p.date)||!/^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/.test(p.src)||p.src.length>12000000)throw Error('Invalid photo.');clean.photos.push({id:p.id,date:p.date,src:p.src,view:['Front','Side','Back'].includes(p.view)?p.view:'Front'})}
 return clean;
}

