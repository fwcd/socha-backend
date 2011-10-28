class TimeEntriesController < ApplicationController
  
  before_filter :fetch_person
  before_filter :fetch_time_entry, :except => [:new, :index, :create]

  access_control do

    action :index, :new do
      allow :tutor
      allow :administrator
    end

    action :edit, :create, :update, :show do
      allow :administrator
      allow :tutor, :if => :own_entry?
    end
  end

  def index
    fetch_filters
    if @contestant
      @tutors_and_helpers = @contestant.tutors_and_helpers
    elsif @season
      @tutors_and_helpers = @season.contestants.map{|r| r.people.tutors_and_helpers}.flatten.uniq
    else
      @tutors_and_helpers = Person.all.select{|p| p.has_role?(:tutor) or p.has_role?(:helper)}.flatten.uniq
    end

    if @filter[:season]
      @contestants = @filter[:season].contestants
    else
      @contestants = Contestant.visible.without_testers
    end

    if @filter[:person] 
      @time_entries = @filter[:person].time_entries
    elsif not @current_user.has_role? :administrator
      @time_entries = @current_user.time_entries
    else
      @time_entries = TimeEntry.all
    end

    if @filter[:contestant]
      @time_entries = @time_entries.select{|t| t.context == @filter[:contestant]}
    end

    if @filter[:season]
      @time_entries = @time_entries.select{|t| (t.context.respond_to?(:season) ? t.context.season : t.context) == @filter[:season] }
    end
  end

  def new
    @time_entry = TimeEntry.new
    respond_to do |format|
      format.js
      format.html
    end
  end

  def edit
    respond_to do |format|
      format.js
      format.html
    end
  end

  def update
    con = params[:time_entry][:context]
    if con 
      con_type, con_id = con.split(":")
      allowed_contexts = ["Contestant","Season","Contest"] 
      if allowed_contexts.include? con_type
        params[:time_entry][:context] = eval(con_type).find_by_id(con_id)
      else
        params[:time_entry][:context] = nil
      end
    end
    if @time_entry.update_attributes(params[:time_entry])
      respond_to do |format|
        format.html do
          flash[:notice] = "#{TimeEntry.human_name} wurde erfolgreich aktualisiert."
          redirect_to :action => :index
        end
        format.js
      end
    else
      respond_to do |format|
        format.html do 
          flash[:error] = "Beim Bearbeiten des #{TimeEntry.human_name}s trat ein Fehler auf!"
        end
        format.js
      end
    end
  end

  def create 
    con = params[:time_entry][:context]
    if con 
      con_type, con_id = con.split(":")
      allowed_contexts = ["Contestant","Season","Contest"] 
      if allowed_contexts.include? con_type
        params[:time_entry][:context] = eval(con_type).find_by_id(con_id)
      else
        params[:time_entry][:context] = nil
      end
    end
    params[:time_entry][:person_id] = @current_user.id
    @time_entry = TimeEntry.create(params[:time_entry])
    if @time_entry.save
      if @time_entry.context.is_a? Season
        contx = @time_entry.context
      elsif @time_entry.context.respond_to? :season
        contx = @time_entry.context.season
      elsif @time_entry.context.resond_to? :parent
        contx = @time_entry.context.parent
      end
      TimeEntryAddedEvent.create(:time_entry => @time_entry, :context => contx, :person => @current_user) 
      respond_to do |format|
        format.html do
          flash[:notice] = "#{TimeEntry.human_name} wurde erfolgreich erstellt."
          redirect_to :action => :index
        end
        format.js
      end
    else
      respond_to do |format|
        format.html do 
          flash[:error] = "Beim Erstellen des #{TimeEntry.human_name}s trat ein Fehler auf!"
        end
        format.js
      end
    end
  end

  def show

  end

  def fetch_person
    @person = Person.find_by_id(params[:person_id])
  end

  def fetch_time_entry
    @time_entry = TimeEntry.find(params[:id])
  end

  def fetch_filters
    @filter = {}
    params[:filter] ||= {}
    @filter[:season] = Season.find_by_id(params[:filter][:season_id])
    @filter[:person] = Person.find_by_id(params[:filter][:person_id])
    @filter[:contestant] = Contestant.find_by_id(params[:filter][:contestant_id])
  end

  def own_entry?
    @time_entry.person == @current_user
  end
end
