package com.mokhovav.meeting_room_reservation.services;

import com.mokhovav.meeting_room_reservation.datatables.Reservation;
import com.mokhovav.meeting_room_reservation.datatables.User;
import com.mokhovav.meeting_room_reservation.responses.DailySchedule;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;

@Service
public class ReservationService {
    final DAOService daoService;
    final Logger logger;

    public ReservationService(DAOService daoService, Logger logger) {
        this.daoService = daoService;
        this.logger = logger;
    }

    public boolean isExist(Long id) {
        return getById(id) != null;
    }

    public boolean save(Reservation reservation){
        if(reservation != null && !reservation.getTitle().isEmpty()) {
            daoService.save(reservation);
            return true;
        }
        return false;
    }

    public boolean update(Reservation reservation) {
        if( reservation!=null && getById(reservation.getId()) != null  && !reservation.getTitle().isEmpty()) {
            daoService.update(reservation);
            return true;
        }
        return false;
    }

    public boolean delete(Long id) {
        Reservation reservation = getById(id);
        if(reservation == null ) return false;
        daoService.delete(reservation);
        return true;
    }

    public Reservation getById(Long id){
        return id > 0 ? (Reservation)daoService.findObject("from Reservation where id="+id) : null;
    }

    public List<Reservation> getAll(){
        return (List<Reservation>)daoService.findAll(Reservation.class);
    }


    public int dayOfWeak() {
        Calendar c = Calendar.getInstance();
        Integer dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek;
    }

    public boolean isValid(User user, String title, String description, Integer duration, String date, String time){
        return user!=null && title !=null && !title.isEmpty() && duration>0 && date!=null && !date.isEmpty() && time!=null && !time.isEmpty()
                && getTimeEnd(Timestamp.valueOf(date + " " + time + ":00").getTime(),duration) < dayTimeEnd(date);
    }

    public boolean isCanBeBooked(Reservation reservation){
        //return true;
        //return daoService.findObject("from Reservation where timeend>'"+reservation.getTimeBegin().toString()+"' and timebegin<'"+reservation.getTimeEnd().toString()+"'") == null;
        return daoService.findObject("from Reservation where timeend>"+reservation.getTimeBegin()+" and timebegin<"+reservation.getTimeEnd()) == null;
    }

    /*public Timestamp getTimeEnd(Timestamp timeBegin, Integer duration){
        long temp = duration % 30 == 0 ?  duration : (duration/30 + 1)*30;
        return new Timestamp(timeBegin.getTime() + temp*60000);
    }*/
    public long getTimeEnd(long timeBegin, Integer duration){
        long temp = duration % 30 == 0 ?  duration : (duration/30 + 1)*30;
        return timeBegin + temp*60000;
    }

    public List<DailySchedule> getSchedule(int startWeak) {
        List<DailySchedule> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();


        int dayOffset = cal.get(Calendar.DAY_OF_WEEK)-2;
        if (dayOffset < 0) dayOffset = 6;
        cal.add(Calendar.DATE, 7*startWeak - dayOffset);

        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        DateFormat dateTimeFormat = new SimpleDateFormat("hh:mm");
        DateFormat dayFormat = new SimpleDateFormat("EEEE");

        List<Reservation> tempList = new ArrayList<>();
        Reservation tempRes = null;
        DailySchedule temp;

        cal.set(Calendar.HOUR,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.AM_PM,Calendar.AM);

        //logger.info("Start time: "+dateTimeFormat.format(cal.getTime()));

        for(int i = 1; i <= 7; i++) {
            logger.info("Start conversion");
           // Map<Long, Reservation> map = new LinkedHashMap<>();
            temp = new DailySchedule();
            temp.dayOfWeek = dayFormat.format(cal.getTime());
            temp.date = dateFormat.format(cal.getTime());

            temp.dayStart = cal.getTimeInMillis();
            temp.dayEnd =  temp.dayStart + 86400000;
            //logger.info("end time: "+ temp.dayEnd );
            //logger.info("calc time: "+ dayTimeEnd("2020-01-06"));
            temp.reservations = new LinkedHashMap<>();


            tempList = daoService.findList("from Reservation where timebegin >= "+temp.dayStart+" and timebegin < "+temp.dayEnd);
            Collections.sort(tempList);
            for (Reservation res : tempList) {
                if (tempRes == null)
                    temp.reservations.put(res.getTimeBegin() - temp.dayStart, res);
                else
                    temp.reservations.put(res.getTimeBegin() - tempRes.getTimeEnd(), res);
                tempRes = res;
            }
            list.add(temp);
            tempRes = null;
            cal.add(Calendar.DATE, 1);
        }
        return list;
    }

    private long dayTimeEnd(String date){
        return Timestamp.valueOf(date + " " + "24:00:00").getTime();
    }
    private long dayTimeStart(String date){
        return Timestamp.valueOf(date + " " + "00:00:00").getTime();
    }
}
